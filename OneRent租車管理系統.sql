/*
Mock Data Alignment Notes (Present Day = 2026-05-15)
1. 修復車輛狀態限制：vehicle 全局只保留 AVAILABLE / RETIRED；租車、專車、調度不再直接把車輛停在 RENTING / SHUTTLING / DISPATCHING / CLEANING / MAINTAINING。
2. 修復二手車資料衝突：used_car 1~6 全部綁定 RETIRED 車輛；所有已有 sales_record 的 used_car 狀態均改為 SOLD，且看車紀錄皆早於成交日或為取消。
3. 修復地點與調度鏈：已完成租車與調度的還車/目的地和 vehicle.location_id 對齊；CLOSED 租車訂單均補齊實際還車時間與里程。
4. 修復接送與司機排班：transfer_order 與 dispatch_log 在 driver_id 的時間區間不重疊；transfer_rate.vehicle_type 已對齊 car_model.vehicle_type。
5. 修復會員點數：customer.current_points 對齊 points_history 淨額；total_accumulated 依 Java PointsService 邏輯只累計正向點數；所有完成租車/專車訂單皆有對應加點紀錄。
6. 修復兌換與票券狀態：redemption_orders 覆蓋 ACTIVE / USED / EXPIRED，voucher 覆蓋 UNUSED / USED / EXPIRED；先移除 CANCELLED 樣本以避免 voucher 無對應取消狀態。
7. 套用 20260515 原始 schema 的 sales_record.pay_status，並補齊 PENDING / PAID / CANCELLED 銷售付款狀態樣本。
8. 補齊主要狀態測試資料：rental_order 僅保留 CLOSED / CANCELLED / RESERVED；transfer_order 僅保留 已完成 / 已取消 / 已預訂；dispatch_log 僅保留 FINISHED / PENDING；5/29 後資料強制為未來預約或待執行。
9. 修復租車訂單價格：依 OrderService 的短租日費+逾時費上限、長租月費*月數、跨站費、10% 訂金規則，重算 1002/1006/1010/1013/1015/1019/1020，並同步修正完成訂單點數。
10. 修復 5/16~5/28 空窗與調度清潔規則：租車/專車/調度排程避開 2026-05-16~2026-05-28；所有調度皆以 scheduled_start_time 為清潔起點，actual_end_time 為 scheduled_start_time + 2 小時。
*/
-- ============================================================
-- 租車管理系統 Car Rental System
-- SQL Server Database Creation Script
-- ============================================================

-- 建立並切換資料庫
IF NOT EXISTS (SELECT name
FROM sys.databases
WHERE name = N'car_rental_db')
    CREATE DATABASE car_rental_db;
GO

USE car_rental_db;
GO

-- ============================================================
-- 一、先卸除所有外鍵約束，再 DROP 所有表格
-- ============================================================

-- 功能5
IF OBJECT_ID(N'sales_record', N'U') IS NOT NULL
BEGIN
    ALTER TABLE sales_record DROP CONSTRAINT IF EXISTS FK_sales_record_usedcar;
    ALTER TABLE sales_record DROP CONSTRAINT IF EXISTS FK_sales_record_cust;
    ALTER TABLE sales_record DROP CONSTRAINT IF EXISTS FK_sales_record_emp;
END;

IF OBJECT_ID(N'viewing_appointment', N'U') IS NOT NULL
BEGIN
    ALTER TABLE viewing_appointment DROP CONSTRAINT IF EXISTS FK_viewing_usedcar;
    ALTER TABLE viewing_appointment DROP CONSTRAINT IF EXISTS FK_viewing_cust;
    ALTER TABLE viewing_appointment DROP CONSTRAINT IF EXISTS FK_viewing_location;
END;

IF OBJECT_ID(N'used_car', N'U') IS NOT NULL
    ALTER TABLE used_car DROP CONSTRAINT IF EXISTS FK_used_car_vehicle;

-- 功能4
IF OBJECT_ID(N'transfer_order', N'U') IS NOT NULL
BEGIN
    ALTER TABLE transfer_order DROP CONSTRAINT IF EXISTS FK_transfer_cust;
    ALTER TABLE transfer_order DROP CONSTRAINT IF EXISTS FK_transfer_driver;
    ALTER TABLE transfer_order DROP CONSTRAINT IF EXISTS FK_transfer_vehicle;
    ALTER TABLE transfer_order DROP CONSTRAINT IF EXISTS FK_transfer_rate;
END;

-- 功能3【voucher 外鍵必須在 redemption_orders 之前卸除】
IF OBJECT_ID(N'voucher', N'U') IS NOT NULL
BEGIN
    ALTER TABLE voucher DROP CONSTRAINT IF EXISTS FK_voucher_redemption;
END;

IF OBJECT_ID(N'points_history', N'U') IS NOT NULL
BEGIN
    ALTER TABLE points_history DROP CONSTRAINT IF EXISTS FK_points_history_cust;
END;

IF OBJECT_ID(N'redemption_orders', N'U') IS NOT NULL
BEGIN
    ALTER TABLE redemption_orders DROP CONSTRAINT IF EXISTS FK_redemption_cust;
    ALTER TABLE redemption_orders DROP CONSTRAINT IF EXISTS FK_redemption_product;
END;

-- 功能2
IF OBJECT_ID(N'short_term_details', N'U') IS NOT NULL
    ALTER TABLE short_term_details DROP CONSTRAINT IF EXISTS FK_short_term_order;

IF OBJECT_ID(N'long_term_details', N'U') IS NOT NULL
    ALTER TABLE long_term_details DROP CONSTRAINT IF EXISTS FK_long_term_order;

IF OBJECT_ID(N'rental_order', N'U') IS NOT NULL
BEGIN
    ALTER TABLE rental_order DROP CONSTRAINT IF EXISTS FK_rental_order_cust;
    ALTER TABLE rental_order DROP CONSTRAINT IF EXISTS FK_rental_order_vehicle;
    ALTER TABLE rental_order DROP CONSTRAINT IF EXISTS FK_rental_order_plan;
    ALTER TABLE rental_order DROP CONSTRAINT IF EXISTS FK_rental_order_pickup;
    ALTER TABLE rental_order DROP CONSTRAINT IF EXISTS FK_rental_order_return;
END;

-- 功能6
IF OBJECT_ID(N'dispatch_log', N'U') IS NOT NULL
BEGIN
    ALTER TABLE dispatch_log DROP CONSTRAINT IF EXISTS FK_dispatch_vehicle;
    ALTER TABLE dispatch_log DROP CONSTRAINT IF EXISTS FK_dispatch_from;
    ALTER TABLE dispatch_log DROP CONSTRAINT IF EXISTS FK_dispatch_to;
    ALTER TABLE dispatch_log DROP CONSTRAINT IF EXISTS FK_dispatch_driver;
    -- 補這行
    ALTER TABLE dispatch_log DROP CONSTRAINT IF EXISTS FK_dispatch_emp;
-- 補這行
END;

IF OBJECT_ID(N'cross_location_fee', N'U') IS NOT NULL
BEGIN
    ALTER TABLE cross_location_fee DROP CONSTRAINT IF EXISTS FK_cross_fee_from;
    ALTER TABLE cross_location_fee DROP CONSTRAINT IF EXISTS FK_cross_fee_to;

END;

IF OBJECT_ID(N'vehicle', N'U') IS NOT NULL
BEGIN
    ALTER TABLE vehicle DROP CONSTRAINT IF EXISTS FK_vehicle_location;
    ALTER TABLE vehicle DROP CONSTRAINT IF EXISTS FK_vehicle_car_model;
--補這行
END;


IF OBJECT_ID(N'car_model', N'U') IS NOT NULL
BEGIN
    ALTER TABLE car_model DROP CONSTRAINT IF EXISTS FK_model_plans;
END;
-- 20260505'1733

-- 功能1
IF OBJECT_ID(N'driver_schedule', N'U') IS NOT NULL
    ALTER TABLE driver_schedule DROP CONSTRAINT IF EXISTS FK_schedule_driver;

IF OBJECT_ID(N'driver', N'U') IS NOT NULL

    BEGIN
    ALTER TABLE driver DROP CONSTRAINT IF EXISTS FK_driver_emp;

    -- >>> 【請把指令加在這裡】 <<<
    ALTER TABLE driver DROP CONSTRAINT IF EXISTS FK_driver_vehicle;
END;

-- ============================================================
-- DROP 所有表格【voucher 必須在 redemption_orders 之前 DROP】
-- ============================================================
DROP TABLE IF EXISTS sales_record;
DROP TABLE IF EXISTS viewing_appointment;
DROP TABLE IF EXISTS used_car;
DROP TABLE IF EXISTS transfer_order;
DROP TABLE IF EXISTS transfer_rate;
DROP TABLE IF EXISTS points_history;
DROP TABLE IF EXISTS points_rules;
DROP TABLE IF EXISTS voucher;
DROP TABLE IF EXISTS redemption_orders;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS short_term_details;
DROP TABLE IF EXISTS long_term_details;
DROP TABLE IF EXISTS rental_order;
DROP TABLE IF EXISTS rental_plans;
DROP TABLE IF EXISTS dispatch_log;
DROP TABLE IF EXISTS cross_location_fee;
DROP TABLE IF EXISTS vehicle;
DROP TABLE IF EXISTS car_model;
--補這行
DROP TABLE IF EXISTS location;
DROP TABLE IF EXISTS driver_schedule;
DROP TABLE IF EXISTS driver;
DROP TABLE IF EXISTS employee;
DROP TABLE IF EXISTS customer;
GO

-- ============================================================
-- 功能6: 車輛管理
-- ============================================================

-- 6-2. location (據點表，VEHICLE 依賴它)
CREATE TABLE location
(
    location_id INT IDENTITY(1,1) NOT NULL,
    location_name NVARCHAR(50) NOT NULL,
    address NVARCHAR(255),
    phone NVARCHAR(20),
    parking_capacity INT,
    location_status NVARCHAR(10),
    -- 營運/暫停/撤點
    is_deleted BIT DEFAULT 0,
    -- 新增：軟刪除標記 (0:正常, 1:已刪除),
    CONSTRAINT PK_location PRIMARY KEY (location_id)
);

-- 2-4. rental_plans
CREATE TABLE rental_plans
(
    plan_id INT IDENTITY(1,1) NOT NULL,
    plan_name NVARCHAR(50),
    applied_vehicle_type NVARCHAR(30),
    --20260511'1835
    plan_desc NVARCHAR(255),
    -- 🌟 新增：方案描述
    is_long_term BIT DEFAULT 0,
    base_price DECIMAL(10,2),
    overtime_fee DECIMAL(10,2),
    mileage_limit INT,
    excess_mileage_fee DECIMAL(10,2),
    is_active BIT DEFAULT 1,
    -- 🌟 新增：上架狀態 (1:上架, 0:下架)
    is_deleted BIT DEFAULT 0,
    created_at DATETIME2,
    -- 由 Java JPA 自動注入
    updated_at DATETIME2,
    -- 由 Java JPA 自動注入

    -- 🌟 修正：必須補上這行，否則後面的 rental_order 無法綁定外部鍵！
    CONSTRAINT PK_rental_plans PRIMARY KEY (plan_id)
);

-- 6-5. car_model(車款表，vehicle父表)
CREATE TABLE car_model
(
    model_id INT IDENTITY(1,1) NOT NULL,
    -- 車款ID
    brand NVARCHAR(50) NOT NULL,
    -- 品牌
    model_name NVARCHAR(50) NOT NULL,
    -- 型號
    displacement INT,
    -- 排氣量
    turning_radius DECIMAL(3,1),
    -- 迴轉半徑
    vehicle_type NVARCHAR(30) NOT NULL,
    -- 車型：小型轎車/中型轎車/休旅車/廂型車/電動車
    seats INT NOT NULL,
    -- 座位數
    luggage_capacity INT,
    -- 行李數
    fuel_type NVARCHAR(10),
    -- 動力：汽油/柴油/純電/油電混合
    transmission NVARCHAR(5),
    -- 變速箱：自排/手排
    wheelchair_available BIT DEFAULT 0,
    -- 輪椅友善
    vehicle_img_url NVARCHAR(255),
    -- 圖片
    --plan_id INT,
    -- 租車方案ID 20260505'1733
    is_deleted BIT DEFAULT 0,
    -- 新增：軟刪除標記 (0:正常, 1:已刪除),
    CONSTRAINT PK_car_model PRIMARY KEY (model_id),
);

-- 6-1. vehicle(car_model子表)
CREATE TABLE vehicle
(
    vehicle_id INT IDENTITY(1,1) NOT NULL,
    -- 車輛ID
    plate_no NVARCHAR(20) NOT NULL,
    -- 車牌號碼
    location_id INT NOT NULL,
    -- 所在據點：車子出廠時，以最後所在據點為主
    status NVARCHAR(30) NOT NULL,
    -- 狀態：CLEANING,場內(整理中)/AVAILABLE,場內(可出租/派車)/RENTING,出租中/ TOBE_MAINTAINED,場內(待維修)/
    -- MAINTAINING,維修中/TOBE_DISPATCHED,場內(待調度)/DISPATCHING,調度中/SHUTTLING,專車接送中/RETIRED,退役待處置
    model_id INT NOT NULL,
    -- 車款ID
    color NVARCHAR(20),
    -- 車色：搭配色碼表做成select選單
    manufacture_date DATE,
    -- 出廠日期
    issued_date DATE NOT NULL,
    -- 發照日期(車齡計算的基準)
    inspection_date DATE,
    -- 法定驗車日(發照日期＋五年，邏輯寫在service)
    mileage INT NOT NULL,
    -- 目前里程數
    next_maintainence_mileage INT,
    -- 下次需維修里程數(通常每10000公里一次，邏輯寫在service)
    description NVARCHAR(MAX),
    -- 車況詳述
    created_time DATETIME2 DEFAULT GETDATE(),
    -- 資料建立時間
    is_deleted BIT DEFAULT 0,
    -- 新增：軟刪除標記 (0:正常, 1:已刪除),
    CONSTRAINT PK_vehicle PRIMARY KEY (vehicle_id),
    CONSTRAINT UQ_vehicle_plate UNIQUE (plate_no),
    CONSTRAINT FK_vehicle_location FOREIGN KEY (location_id) REFERENCES location(location_id),
    CONSTRAINT FK_vehicle_car_model FOREIGN KEY (model_id) REFERENCES car_model(model_id)
);


-- 6-3. dispatch_log
CREATE TABLE dispatch_log
(
    dispatch_id INT IDENTITY(1,1) NOT NULL,
    vehicle_id INT,
    from_location_id INT,
    to_location_id INT,
    driver_id INT,
    scheduled_start_time DATETIME2,
    actual_start_time DATETIME2,
    actual_end_time DATETIME2,
    start_mileage INT,
    end_mileage INT,
    emp_id INT,
    reason NVARCHAR(200),
    status NVARCHAR(10),
    -- 待執行/調度中/已完成/已取消
    notes NVARCHAR(MAX),
    created_at DATETIME2,
    updated_at DATETIME2,
    is_deleted BIT DEFAULT 0,
    -- 新增：軟刪除標記 (0:正常, 1:已刪除),
    CONSTRAINT PK_dispatch_log PRIMARY KEY (dispatch_id)
);

-- 6-4. cross_location_fee
CREATE TABLE cross_location_fee
(
    fee_id INT IDENTITY(1,1) NOT NULL,
    from_location_id INT,
    to_location_id INT,
    extra_fee DECIMAL(10,2),
    is_deleted BIT DEFAULT 0,
    -- 新增：軟刪除標記 (0:正常, 1:已刪除),
    CONSTRAINT PK_cross_location_fee PRIMARY KEY (fee_id)
);

-- ============================================================
-- 功能1: 員工/用戶登入
-- ============================================================

-- 1-2. employee
CREATE TABLE employee
(
    emp_id INT IDENTITY(1,1) NOT NULL,
    emp_name NVARCHAR(50),
    emp_account NVARCHAR(50),
    emp_password NVARCHAR(255),
    emp_role NVARCHAR(10)
        CONSTRAINT CK_emp_role
            CHECK (emp_role IN (N'ADMIN', N'DRIVER', N'USER')),
    emp_phone NVARCHAR(20),
    emp_email NVARCHAR(100),
    emp_status NVARCHAR(5)
        CONSTRAINT CK_emp_status
            CHECK (emp_status IN (N'在職', N'離職')),
    -- 5/20 改為在職/離職
    emp_photo NVARCHAR(500),
    resign_date DATE NULL,
    -- 5/20 新增：離職日期（在職時為 NULL）
    created_at DATETIME2,
    updated_at DATETIME2,
    CONSTRAINT PK_employee PRIMARY KEY (emp_id)
);


-- 1-4. driver
CREATE TABLE driver
(
    driver_id INT IDENTITY(1,1) NOT NULL,
    emp_id INT,
    fixed_vehicle_id INT,
    -- 【新增】用來固定綁定車輛
    license_no NVARCHAR(20),
    license_expiry DATETIME2,
    pet_available BIT DEFAULT 0,
    CONSTRAINT PK_driver PRIMARY KEY (driver_id),
    CONSTRAINT FK_driver_emp FOREIGN KEY (emp_id) REFERENCES employee(emp_id)
);

-- 1-1. driver_schedule
-- CREATE TABLE driver_schedule
-- (
--     schedule_id INT IDENTITY(1,1) NOT NULL,
--     driver_id INT,
--     work_date DATE,
--     shift_type NVARCHAR(5),
--     -- 早班/晚班/休假
--     shift_start DATETIME2,
--     shift_end DATETIME2,
--     notes NVARCHAR(100),
--     CONSTRAINT PK_driver_schedule PRIMARY KEY (schedule_id),
--     CONSTRAINT FK_schedule_driver FOREIGN KEY (driver_id) REFERENCES driver(driver_id)
-- )

-- 1-3. customer
CREATE TABLE customer
(
    cust_id INT IDENTITY(1,1) NOT NULL,
    cust_name NVARCHAR(50),
    cust_phone NVARCHAR(20),
    cust_email NVARCHAR(100),
    cust_account NVARCHAR(50),
    cust_password NVARCHAR(255),
    cust_address NVARCHAR(255),
    cust_license NVARCHAR(20),
    cust_license_expiry DATE,
    -- 啟用 / 停權
    cust_status NVARCHAR(10),
    current_points INT DEFAULT 0,
    total_accumulated INT DEFAULT 0,
    -- 忘記密碼時
    reset_token VARCHAR(255) NULL,
    reset_token_expiry DATETIME NULL,
    -- 5/19圖片上傳路徑
    cust_avatar NVARCHAR(255) NULL,
    CONSTRAINT PK_customer PRIMARY KEY (cust_id)
);


-- ============================================================
-- 功能2: 租車系統
-- ============================================================

-- 2-1. rental_order
-- 【組長 0504 新增】order_remark、invoice_no 兩個欄位
CREATE TABLE rental_order
(
    order_id INT IDENTITY(1,1) NOT NULL,
    cust_id INT,
    vehicle_id INT,
    order_type NVARCHAR(50),
    -- 日租/長租
    plan_id INT,
    pickup_location_id INT,
    return_location_id INT,
    pickup_time DATETIME2,
    return_time DATETIME2,
    rental_fee DECIMAL(12,2),
    extra_fee DECIMAL(10,2),
    deposit DECIMAL(12,2),
    total_amount DECIMAL(12,2),
    pay_status NVARCHAR(50),
    -- 已付訂金/未付訂金/支付完成
    order_time DATETIME2,
    order_status NVARCHAR(50),
    -- 已預約/已取車/已歸還(待檢查)/已結案/已取消

    -- 🌟 修改：拆分付款方式，並將長度放寬至 20 預留給 Enum 字串
    deposit_pay_method NVARCHAR(50),
    -- 訂金付款方式 (信用卡/轉帳/行動支付等)
    balance_pay_method NVARCHAR(50),
    -- 尾款付款方式 (信用卡/轉帳/現金/行動支付等)

    order_remark NVARCHAR(255),
    -- 【組長 0504 新增】客戶備註
    invoice_no NVARCHAR(50),
    -- 【組長 0504 新增】發票號碼（取代原 invoice_id INT）
    contract NVARCHAR(255),
    updated_at DATETIME2,
    is_deleted BIT DEFAULT 0,
    -- 新增：軟刪除標記 (0:正常, 1:已刪除),
    CONSTRAINT PK_rental_order PRIMARY KEY (order_id),
    CONSTRAINT FK_rental_order_cust    FOREIGN KEY (cust_id)            REFERENCES customer(cust_id),
    CONSTRAINT FK_rental_order_vehicle FOREIGN KEY (vehicle_id)         REFERENCES vehicle(vehicle_id),
    CONSTRAINT FK_rental_order_plan    FOREIGN KEY (plan_id)            REFERENCES rental_plans(plan_id),
    CONSTRAINT FK_rental_order_pickup  FOREIGN KEY (pickup_location_id) REFERENCES location(location_id),
    CONSTRAINT FK_rental_order_return  FOREIGN KEY (return_location_id) REFERENCES location(location_id)
);

-- 2-2. short_term_details
CREATE TABLE short_term_details
(
    detail_id INT IDENTITY(1,1) NOT NULL,
    order_id INT,
    actual_pickup_time DATETIME2,
    actual_return_time DATETIME2,
    start_mileage INT,
    end_mileage INT,
    note_desc NVARCHAR(MAX),
    fuel_level_return NVARCHAR(20),
    is_deleted BIT DEFAULT 0,
    -- 新增：軟刪除標記 (0:正常, 1:已刪除),
    created_at DATETIME2,
    --  補上
    updated_at DATETIME2,
    --  補上
    CONSTRAINT PK_short_term_details PRIMARY KEY (detail_id),
    CONSTRAINT FK_short_term_order FOREIGN KEY (order_id) REFERENCES rental_order(order_id)
);

-- 2-3. long_term_details
CREATE TABLE long_term_details
(
    detail_id INT IDENTITY(1,1) NOT NULL,
    order_id INT,
    actual_pickup_time DATETIME2,
    actual_return_time DATETIME2,
    contract_months INT,
    monthly_payment DECIMAL(12,2),
    billing_day INT,
    paid_months INT DEFAULT 0,
    delivery_address NVARCHAR(255),
    start_mileage INT,
    end_mileage INT,
    note_desc NVARCHAR(MAX),
    is_deleted BIT DEFAULT 0,
    -- 新增：軟刪除標記 (0:正常, 1:已刪除),
    created_at DATETIME2,
    updated_at DATETIME2,
    CONSTRAINT PK_long_term_details PRIMARY KEY (detail_id),
    CONSTRAINT FK_long_term_order FOREIGN KEY (order_id) REFERENCES rental_order(order_id)
);

-- ============================================================
-- 功能3: 點數兌換專區
-- ============================================================

-- 3-1. product
-- 【修改】product_name/points_required/stock_quantity/is_active 加 NOT NULL
-- 【新增】category 欄位（汽車配件/加值服務/禮品兌換/合作品牌）
CREATE TABLE product
(
    product_id INT IDENTITY(1,1) NOT NULL,
    product_name NVARCHAR(100) NOT NULL,
    description NVARCHAR(MAX),
    points_required INT NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    is_active BIT NOT NULL DEFAULT 1,
    image_url NVARCHAR(MAX),
    category NVARCHAR(20),
    -- 汽車配件/加值服務/禮品兌換/合作品牌
    CONSTRAINT PK_product PRIMARY KEY (product_id)
);

-- 3-2. redemption_orders
-- 【修改】多欄加 NOT NULL
-- 【新增】update_time 欄位（允許 NULL）
-- 【修改】order_status 使用英文常數：ACTIVE / USED / CANCELLED / EXPIRED
CREATE TABLE redemption_orders
(
    redemption_id INT IDENTITY(1,1) NOT NULL,
    cust_id INT NOT NULL,
    product_id INT NOT NULL,
    product_quantity INT NOT NULL DEFAULT 1,
    points_spent INT NOT NULL,
    order_status NVARCHAR(10) NOT NULL,
    -- ACTIVE / USED / CANCELLED / EXPIRED
    create_time DATETIME2 NOT NULL,
    update_time DATETIME2,
    -- 狀態更新時自動設定，建立時為 NULL
    CONSTRAINT PK_redemption_orders PRIMARY KEY (redemption_id),
    CONSTRAINT FK_redemption_cust    FOREIGN KEY (cust_id)    REFERENCES customer(cust_id),
    CONSTRAINT FK_redemption_product FOREIGN KEY (product_id) REFERENCES product(product_id)
);

-- 3-3. points_history
-- 【修改】多欄加 NOT NULL
-- 【新增】expire_time 欄位（NOT NULL）
-- 【修改】change_type 使用英文常數：RENTAL / TRANSFER / BIRTHDAY / EXPIRED / REDEMPTION
-- 【說明】reference_id 不設外鍵（多態關聯，可指向不同表）
CREATE TABLE points_history
(
    record_id INT IDENTITY(1,1) NOT NULL,
    cust_id INT NOT NULL,
    remain_points INT NOT NULL,
    change_type NVARCHAR(50) NOT NULL,
    -- RENTAL / TRANSFER / BIRTHDAY / EXPIRED / REDEMPTION
    points_change INT NOT NULL,
    reference_id NVARCHAR(50),
    -- 允許 NULL（生日贈送等無對應單號）
    notes NVARCHAR(MAX),
    create_time DATETIME2 NOT NULL,
    expire_time DATETIME2 NOT NULL,
    -- 點數到期日，insertPointsHistory 自動設定為 +365天
    available_points INT NULL,
    -- FIFO 扣點用：獲得點數紀錄的剩餘可扣量；消耗/過期類為 NULL
    CONSTRAINT PK_points_history PRIMARY KEY (record_id),
    CONSTRAINT FK_points_history_cust FOREIGN KEY (cust_id) REFERENCES customer(cust_id)
);

-- 3-4. points_rules
-- 【修改】多欄加 NOT NULL
CREATE TABLE points_rules
(
    rule_id INT IDENTITY(1,1) NOT NULL,
    rule_name NVARCHAR(100) NOT NULL,
    rule_key NVARCHAR(50) NOT NULL,
    ratio DECIMAL(10,4) NOT NULL,
    description NVARCHAR(MAX),
    is_active BIT NOT NULL DEFAULT 1,
    CONSTRAINT PK_points_rules PRIMARY KEY (rule_id)
);

-- 3-5. voucher
-- 【新增】每筆兌換訂單對應一或多張票券，每張票券有獨立序號與狀態
-- status：UNUSED（未使用）/ USED（已使用）/ EXPIRED（已過期）
-- use_time：允許 NULL，使用後記錄時間
-- expiry_date：票券到期日，由 insertRedemptionOrder 自動設定為兌換時間 +1年
CREATE TABLE voucher
(
    voucher_id INT IDENTITY(1,1) NOT NULL,
    redemption_id INT NOT NULL,
    -- 外鍵，關聯 redemption_orders
    voucher_code NVARCHAR(50) NOT NULL,
    -- UUID 產生，全域唯一
    status NVARCHAR(10) NOT NULL DEFAULT 'UNUSED',
    -- UNUSED / USED / EXPIRED
    use_time DATETIME2 NULL,
    -- 使用時間，未使用時為 NULL
    expiry_date DATETIME2 NOT NULL,
    -- 票券到期日
    CONSTRAINT PK_voucher PRIMARY KEY (voucher_id),
    CONSTRAINT UQ_voucher_code UNIQUE (voucher_code),
    CONSTRAINT FK_voucher_redemption FOREIGN KEY (redemption_id) REFERENCES redemption_orders(redemption_id)
);

-- ============================================================
-- 功能4: 專車接送
-- ============================================================

-- 4-2. transfer_rate
CREATE TABLE transfer_rate
(
    rate_id INT IDENTITY(1,1) NOT NULL,
    rate_name NVARCHAR(100),
    base_fee DECIMAL(10,2),
    per_km_fee DECIMAL(10,2),
    vehicle_type NVARCHAR(30),
    is_active TINYINT DEFAULT 1,
    created_at DATETIME2,
    CONSTRAINT PK_transfer_rate PRIMARY KEY (rate_id)
);

-- 4-1. transfer_order
CREATE TABLE transfer_order
(
    transfer_id INT IDENTITY(1,1) NOT NULL,
    cust_id INT,
    cust_phone NVARCHAR(20),
    driver_id INT,
    vehicle_id INT,
    rate_id INT,
    transfer_type NVARCHAR(5),
    -- 接機/送機/一般
    pickup_location NVARCHAR(255),
    dropoff_location NVARCHAR(255),
    scheduled_pickup_time DATETIME2,
    scheduled_dropoff_time DATETIME2,
    real_dropoff_time DATETIME2,
    passenger_count INT,
    start_mileage INT,
    end_mileage INT,
    luggage_count INT,
    total_amount DECIMAL(10,2),
    status NVARCHAR(10),
    -- 已預訂/接送中/已完成/已取消
    note NVARCHAR(MAX),
    created_at DATETIME2,
    CONSTRAINT PK_transfer_order PRIMARY KEY (transfer_id),
    CONSTRAINT FK_transfer_cust    FOREIGN KEY (cust_id)    REFERENCES customer(cust_id),
    CONSTRAINT FK_transfer_driver  FOREIGN KEY (driver_id)  REFERENCES driver(driver_id),
    CONSTRAINT FK_transfer_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id),
    CONSTRAINT FK_transfer_rate    FOREIGN KEY (rate_id)    REFERENCES transfer_rate(rate_id)
);

-- ============================================================
-- 功能5: 二手車出售
-- ============================================================

-- 5-2. used_car
CREATE TABLE used_car
(
    usedcar_id INT IDENTITY(1,1) NOT NULL,
    vehicle_id INT,
    asking_price DECIMAL(12,2),
    condition_desc NVARCHAR(MAX),
    list_date DATE,
    expire_date DATE,
    status NVARCHAR(50),
    --ACTIVE,上架 /SOLD,已售/REMOVED,下架
    created_time DATETIME2,
    is_deleted BIT DEFAULT 0,
    -- 新增：軟刪除標記 (0:正常, 1:已刪除),
    CONSTRAINT PK_used_car PRIMARY KEY (usedcar_id),
    CONSTRAINT FK_used_car_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id)
);

-- 5-1. viewing_appointment
CREATE TABLE viewing_appointment
(
    appt_id INT IDENTITY(1,1) NOT NULL,
    usedcar_id INT,
    cust_id INT,
    appt_time DATETIME2,
    status NVARCHAR(255),
    --PENDING,待確認（剛收到資料）/CONFIRMED,已預定(聯繫過客戶）/
    --COMPLETED，已完成看車/CANCELLED,已取消
    location_id INT,
    message NVARCHAR(MAX),
    notes NVARCHAR(MAX),
    is_deleted BIT DEFAULT 0,
    -- 新增：軟刪除標記 (0:正常, 1:已刪除),
    CONSTRAINT PK_viewing_appointment PRIMARY KEY (appt_id),
    CONSTRAINT FK_viewing_usedcar  FOREIGN KEY (usedcar_id)  REFERENCES used_car(usedcar_id),
    CONSTRAINT FK_viewing_cust     FOREIGN KEY (cust_id)     REFERENCES customer(cust_id),
    CONSTRAINT FK_viewing_location FOREIGN KEY (location_id) REFERENCES location(location_id)
);

-- 5-3. sales_record
CREATE TABLE sales_record
(
    sale_id INT IDENTITY(1,1) NOT NULL,
    usedcar_id INT,
    cust_id INT,
    buyer_name NVARCHAR(50),
    buyer_phone NVARCHAR(20),
    buyer_idno NVARCHAR(20),
    final_price DECIMAL(12,2),
    payment_method NVARCHAR(50),
    -- CASH("現金"),CREDIT_CARD("信用卡"),TRANSFER("轉帳"),
    --0512新增付款狀態(串綠界用)
    pay_status NVARCHAR(20) DEFAULT N'PENDING',
    -- PENDING: 待付款, PAID: 已付款, CANCELLED: 已取消
    sale_date DATE,
    emp_id INT,
    notes NVARCHAR(100),
    is_deleted BIT DEFAULT 0,
    -- 新增：軟刪除標記 (0:正常, 1:已刪除),
    CONSTRAINT PK_sales_record PRIMARY KEY (sale_id),
    CONSTRAINT FK_sales_record_usedcar FOREIGN KEY (usedcar_id) REFERENCES used_car(usedcar_id),
    CONSTRAINT FK_sales_record_cust    FOREIGN KEY (cust_id)    REFERENCES customer(cust_id),
    CONSTRAINT FK_sales_record_emp     FOREIGN KEY (emp_id)     REFERENCES employee(emp_id)
);

-- 補上 dispatch_log 與 cross_location_fee 的外鍵 (需等 driver、location 建完後加入)
ALTER TABLE dispatch_log
    ADD CONSTRAINT FK_dispatch_vehicle FOREIGN KEY (vehicle_id)       REFERENCES vehicle(vehicle_id),
        CONSTRAINT FK_dispatch_from    FOREIGN KEY (from_location_id) REFERENCES location(location_id),
        CONSTRAINT FK_dispatch_to      FOREIGN KEY (to_location_id)   REFERENCES location(location_id),
        CONSTRAINT FK_dispatch_driver  FOREIGN KEY (driver_id)        REFERENCES driver(driver_id),
        CONSTRAINT FK_dispatch_emp     FOREIGN KEY (emp_id)           REFERENCES employee(emp_id);

ALTER TABLE cross_location_fee
    ADD CONSTRAINT FK_cross_fee_from FOREIGN KEY (from_location_id) REFERENCES location(location_id),
        CONSTRAINT FK_cross_fee_to   FOREIGN KEY (to_location_id)   REFERENCES location(location_id);

GO

ALTER TABLE driver 
    ADD CONSTRAINT FK_driver_vehicle FOREIGN KEY (fixed_vehicle_id) REFERENCES vehicle(vehicle_id);
GO

--ALTER TABLE car_model
--ADD CONSTRAINT FK_model_plans FOREIGN KEY (plan_id) REFERENCES rental_plans(plan_id);
--GO
-- 20260505'1733

-- ============================================================
-- 範例資料 (INSERT INTO) - 依照依賴順序插入
-- Present Day 固定為 2026-03-15
-- ============================================================

-- ── location ─────────────────────────────────────────────────
-- OPERATING,營運中/REST,暫停營業/CANCEL,撤銷據點
SET IDENTITY_INSERT location ON;
INSERT INTO location
    (location_id, location_name, address, phone, parking_capacity, location_status)
VALUES
    (1, N'台北總站', N'台北市中正區忠孝西路一段1號', N'02-2311-0000', 30, N'OPERATING'),
    (2, N'新竹站', N'新竹市東區中華路一段1號', N'03-5221-0000', 20, N'OPERATING');
SET IDENTITY_INSERT location OFF;

-- ── rental_plans ─────────────────────────────────────────────
SET IDENTITY_INSERT rental_plans ON;
INSERT INTO rental_plans
    (plan_id, plan_name, applied_vehicle_type, plan_desc, is_long_term, base_price, overtime_fee, mileage_limit, excess_mileage_fee, is_active, created_at, updated_at)
VALUES
    -- 🟢 日租方案 (1~5)
    (1, N'日租-小型轎車', N'小型轎車', N'適合市區代步的小資選擇', 0, 1500.00, 200.00, 200, 10.00, 1, '2024-10-01 10:00:00', '2026-03-15 00:00:00'),
    (2, N'日租-中型轎車', N'中型轎車', N'舒適平穩的標準房車', 0, 2000.00, 250.00, 250, 10.00, 1, '2024-10-01 10:00:00', '2026-03-15 00:00:00'),
    (3, N'日租-休旅車', N'休旅車', N'家庭出遊大空間首選', 0, 2500.00, 300.00, 300, 12.00, 1, '2024-10-01 10:00:00', '2026-03-15 00:00:00'),
    (4, N'日租-廂型車', N'廂型車', N'多人成行或載物好幫手', 0, 3000.00, 350.00, 300, 15.00, 1, '2024-10-01 10:00:00', '2026-03-15 00:00:00'),
    (5, N'日租-電動車', N'電動車', N'科技環保的極致尊榮體驗', 0, 3500.00, 400.00, 250, 15.00, 1, '2024-10-01 10:00:00', '2026-03-15 00:00:00'),
    -- 🔵 長租方案 (6~10)
    (6, N'長租-小型轎車', N'小型轎車', N'業務長期代步入門款', 1, 18000.00, 0.00, 5000, 8.00, 1, '2024-10-01 10:00:00', '2026-03-15 00:00:00'),
    (7, N'長租-中型轎車', N'中型轎車', N'企業員工公務標準配車', 1, 21000.00, 0.00, 6000, 7.00, 1, '2024-10-01 10:00:00', '2026-03-15 00:00:00'),
    (8, N'長租-休旅車', N'休旅車', N'主管配車或長期家庭出遊', 1, 25000.00, 0.00, 8000, 6.00, 1, '2024-10-01 10:00:00', '2026-03-15 00:00:00'),
    (9, N'長租-廂型車', N'廂型車', N'企業接駁與商務載客用途', 1, 28000.00, 0.00, 9000, 6.00, 1, '2024-10-01 10:00:00', '2026-03-15 00:00:00'),
    (10, N'長租-電動車', N'電動車', N'總裁級旗艦環保座駕', 1, 30000.00, 0.00, 10000, 5.00, 1, '2024-10-01 10:00:00', '2026-03-15 00:00:00'),
    -- 🌟 客製化逃生門 (依照要求放在第 11 項)
    (11, N'其他 (客製化專案)', N'其他', N'特殊報價、活動包車或無法歸類之特例', 0, 0.00, 0.00, 0, 0.00, 1, '2024-10-01 10:00:00', '2026-03-15 00:00:00');
SET IDENTITY_INSERT rental_plans OFF;

-- ── car_model ──────────────────────────────────────────────────
SET IDENTITY_INSERT car_model ON;
INSERT INTO car_model
    (model_id, brand, model_name, displacement, turning_radius, vehicle_type, seats, luggage_capacity, fuel_type, transmission, wheelchair_available, vehicle_img_url)
VALUES
    (1, N'TOYOTA', N'Altis', 1800, 3.1, N'中型轎車', 5, 3, N'油電混合', N'自排', 0, 'http://localhost:8081/uploads/altis.png'),
    (2, N'TOYOTA', N'Sienta', 1798, 3.6, N'休旅車', 7, 2, N'汽油', N'自排', 0, 'http://localhost:8081/uploads/sienta.png'),
    (3, N'TOYOTA', N'Yaris', 1500, 3.1, N'小型轎車', 5, 2, N'油電混合', N'自排', 0, 'http://localhost:8081/uploads/yaris.png'),
    (4, N'TOYOTA', N'GRANVIA', 2755, 4.1, N'廂型車', 8, 8, N'柴油', N'自排', 0, 'http://localhost:8081/uploads/granvia.png'),
    (5, N'Tesla', N'Model 3', 0, 5.8, N'電動車', 5, 3, N'電動', N'自排', 0, 'http://localhost:8081/uploads/model3.png'),
    (6, N'HONDA', N'CR-V', 1498, 5.5, N'休旅車', 5, 3, N'油電混合', N'自排', 0, 'http://localhost:8081/uploads/crv.png');
SET IDENTITY_INSERT car_model OFF;

-- ── vehicle ──────────────────────────────────────────────────
SET IDENTITY_INSERT vehicle ON;
INSERT INTO vehicle
    (vehicle_id, plate_no, location_id, status, model_id, color, manufacture_date, issued_date, inspection_date, mileage, next_maintainence_mileage, description, created_time)
VALUES
    (1, N'RAC-0001', 1, N'AVAILABLE', 1, N'黑色', '2020-11-20', '2021-12-13', '2026-12-13', 30620, 40000, N'全局狀態限制：可用車，供未來預約或已完成訂單後停放', '2022-01-20 11:00:00'),
    (2, N'RAC-0002', 1, N'AVAILABLE', 2, N'綠色', '2023-01-10', '2023-01-25', '2028-01-25', 45120, 50000, N'全局狀態限制：未來長租 1003 仍以 AVAILABLE 呈現', '2023-02-20 11:00:00'),
    (3, N'RAC-0003', 1, N'AVAILABLE', 3, N'銀色', '2021-11-10', '2021-11-24', '2026-11-24', 53210, 60000, N'全局狀態限制：可用車，歷史租車已終局', '2022-01-20 11:00:00'),
    (4, N'RAC-0004', 1, N'RETIRED', 4, N'黑色', '2021-06-05', '2021-06-27', '2026-06-27', 66820, 70000, N'退役並已售出', '2021-07-20 11:00:00'),
    (5, N'RAC-0005', 1, N'AVAILABLE', 5, N'紅色', '2024-03-04', '2024-06-15', '2029-06-15', 37000, 40000, N'全局狀態限制：未來調度待執行，車輛仍為可用', '2024-08-20 11:00:00'),
    (6, N'RAC-0006', 1, N'AVAILABLE', 6, N'白色', '2024-05-24', '2024-06-14', '2029-06-14', 28000, 30000, N'全局狀態限制：專車預約不預先鎖成接送中', '2024-08-20 11:00:00'),
    (7, N'RAC-0007', 2, N'AVAILABLE', 1, N'白色', '2022-06-18', '2022-07-03', '2027-07-03', 45300, 50000, N'全局狀態限制：歷史訂單已終局，車輛可用', '2023-01-20 11:00:00'),
    (8, N'RAC-0008', 2, N'RETIRED', 2, N'綠色', '2023-09-15', '2023-10-04', '2028-10-04', 42100, 50000, N'退役並已售出', '2023-10-20 11:00:00'),
    (9, N'RAC-0009', 1, N'RETIRED', 3, N'銀色', '2022-02-01', '2022-03-01', '2027-03-01', 61200, 70000, N'退役並已售出', '2022-06-20 11:00:00'),
    (10, N'RAC-0010', 2, N'RETIRED', 4, N'黑色', '2022-08-19', '2022-09-10', '2027-09-10', 49800, 60000, N'退役並已售出', '2023-01-20 11:00:00'),
    (11, N'RAC-0011', 2, N'RETIRED', 5, N'紅色', '2025-01-12', '2025-04-15', '2030-04-15', 26300, 30000, N'退役並已售出', '2025-08-20 11:00:00'),
    (12, N'RAC-0012', 1, N'RETIRED', 6, N'白色', '2022-10-23', '2022-11-16', '2027-11-16', 56600, 60000, N'退役並已售出', '2023-01-20 11:00:00'),
    (13, N'RAC-0013', 1, N'AVAILABLE', 3, N'藍色', '2024-01-08', '2024-02-01', '2029-02-01', 18500, 20000, N'新增司機固定車，無未完成訂單撞期', '2024-02-20 11:00:00'),
    (14, N'RAC-0014', 2, N'AVAILABLE', 4, N'灰色', '2023-12-10', '2024-01-10', '2029-01-10', 24600, 30000, N'全局狀態限制：未來專車預約仍顯示可用', '2024-01-20 11:00:00'),
    (15, N'RAC-0015', 1, N'AVAILABLE', 1, N'藍色', '2023-05-01', '2023-06-01', '2028-06-01', 18920, 20000, N'全局狀態限制：歷史租車已終局，車輛可用', '2023-06-20 11:00:00'),
    (16, N'RAC-0016', 2, N'AVAILABLE', 5, N'白色', '2022-04-12', '2022-05-01', '2027-05-01', 60300, 70000, N'全局狀態限制：不使用維修中狀態，保留為可用測試車', '2022-05-20 11:00:00'),
    (17, N'RAC-0017', 1, N'RETIRED', 1, N'銀色', '2020-03-10', '2020-04-01', '2025-04-01', 95800, 100000, N'退役待售車', '2020-04-20 11:00:00'),
    (18, N'RAC-0018', 2, N'RETIRED', 2, N'白色', '2020-05-12', '2020-06-01', '2025-06-01', 88200, 90000, N'退役待售車', '2020-06-20 11:00:00'),
    (19, N'RAC-0019', 1, N'RETIRED', 3, N'紅色', '2019-11-20', '2019-12-15', '2024-12-15', 103500, 110000, N'退役下架樣本', '2019-12-20 11:00:00'),
    (20, N'RAC-0020', 2, N'RETIRED', 4, N'黑色', '2020-08-19', '2020-09-10', '2025-09-10', 91000, 100000, N'退役已售樣本', '2020-09-20 11:00:00'),
    (21, N'RAC-0021', 2, N'AVAILABLE', 3, N'藍色', '2024-02-01', '2024-03-01', '2029-03-01', 15200, 20000, N'完成租車與調度後停放新竹站', '2024-03-20 11:00:00'),
    (22, N'RAC-0022', 2, N'AVAILABLE', 2, N'棕色', '2024-04-10', '2024-05-01', '2029-05-01', 18000, 20000, N'未來調度待執行樣本', '2024-05-20 11:00:00'),
    (23, N'RAC-0023', 1, N'AVAILABLE', 5, N'白色', '2024-06-15', '2024-07-01', '2029-07-01', 21000, 30000, N'取消調度後可用', '2024-07-20 11:00:00'),
    (24, N'RAC-0024', 1, N'AVAILABLE', 5, N'黑色', '2024-07-20', '2024-08-10', '2029-08-10', 27000, 30000, N'全局狀態限制：未來調度待執行，車輛仍為可用', '2024-08-20 11:00:00'),
    (25, N'RAC-0025', 1, N'AVAILABLE', 6, N'銀色', '2023-03-03', '2023-04-01', '2028-04-01', 22400, 30000, N'全局狀態限制：未來租車預約不預先鎖成出租中', '2023-04-20 11:00:00'),
    (26, N'RAC-0026', 1, N'AVAILABLE', 1, N'綠色', '2023-08-08', '2023-09-01', '2028-09-01', 19360, 20000, N'全局狀態限制：歷史租車已終局，車輛可用', '2023-09-20 11:00:00'),
    (27, N'RAC-0027', 2, N'AVAILABLE', 2, N'灰色', '2022-12-12', '2023-01-10', '2028-01-10', 70500, 80000, N'全局狀態限制：不使用維修中狀態，保留為可用測試車', '2023-01-20 11:00:00'),
    (28, N'RAC-0028', 1, N'AVAILABLE', 1, N'白色', '2024-09-01', '2024-10-01', '2029-10-01', 11000, 20000, N'未來短租與專車預約用車', '2024-10-20 11:00:00'),
    (29, N'RAC-0029', 2, N'AVAILABLE', 4, N'銀色', '2024-10-10', '2024-11-01', '2029-11-01', 9500, 20000, N'未來商旅專車預約用車', '2024-11-20 11:00:00'),
    (30, N'RAC-0030', 2, N'AVAILABLE', 2, N'黑色', '2023-10-01', '2023-11-01', '2028-11-01', 34000, 40000, N'全局狀態限制：未來長租預約不預先鎖成出租中', '2023-11-20 11:00:00'),
    (31, N'RAC-0031', 2, N'RETIRED', 2, N'白色', '2020-10-01', '2021-11-01', '2025-11-01', 90000, 100000, N'全局狀態限制：未來長租預約不預先鎖成出租中', '2021-11-20 11:00:00'),
    (32, N'RAC-0032', 1, N'RETIRED', 4, N'白色', '2020-10-01', '2021-11-01', '2025-11-01', 90000, 100000, N'全局狀態限制：未來長租預約不預先鎖成出租中', '2021-11-20 11:00:00');
SET IDENTITY_INSERT vehicle OFF;

-- ── employee ─────────────────────────────────────────────────
SET IDENTITY_INSERT employee ON;
INSERT INTO employee
    (emp_id, emp_name, emp_account, emp_password, emp_role, emp_phone, emp_email, emp_status, emp_photo, resign_date, created_at, updated_at)
VALUES
    -- 在職員工（resign_date = NULL）
    (1, N'王大明', N'admin001', N'hashed_pw_001', N'ADMIN', N'0912-111-111', N'wang@car.com', N'在職', NULL, NULL, '2022-01-01 09:00:00', '2026-03-15 00:00:00'),
    (2, N'林小花', N'staff001', N'hashed_pw_002', N'USER', N'0922-222-222', N'lin@car.com', N'在職', NULL, NULL, '2022-03-15 09:00:00', '2026-03-15 00:00:00'),
    (3, N'陳阿土', N'driver01', N'hashed_pw_003', N'DRIVER', N'0933-333-333', N'chen@car.com', N'在職', NULL, NULL, '2022-06-01 09:00:00', '2026-03-15 00:00:00'),
    (4, N'黃志遠', N'driver02', N'hashed_pw_004', N'DRIVER', N'0944-444-444', N'huang@car.com', N'在職', NULL, NULL, '2023-01-10 09:00:00', '2026-03-15 00:00:00'),
    (5, N'張美玲', N'driver03', N'hashed_pw_005', N'DRIVER', N'0955-555-555', N'chang@car.com', N'在職', NULL, NULL, '2023-04-01 09:00:00', '2026-03-15 00:00:00'),
    (6, N'吳建國', N'driver04', N'hashed_pw_006', N'DRIVER', N'0966-666-666', N'wu@car.com', N'在職', NULL, NULL, '2021-09-01 09:00:00', '2026-03-15 00:00:00'),
    (7, N'周志豪', N'driver05', N'hashed_pw_007', N'DRIVER', N'0977-777-777', N'chou@car.com', N'在職', NULL, NULL, '2024-01-15 09:00:00', '2026-03-15 00:00:00'),
    (8, N'鄭雅雯', N'driver06', N'hashed_pw_008', N'DRIVER', N'0988-888-888', N'cheng@car.com', N'在職', NULL, NULL, '2024-02-01 09:00:00', '2026-03-15 00:00:00'),
    -- 離職員工（emp_status='離職'，resign_date 填入離職日）
    (9, N'高國強', N'staff003', N'hashed_pw_009', N'USER', N'0999-999-999', N'kao@car.com', N'離職', NULL, '2025-08-01', '2023-07-01 09:00:00', '2025-08-01 00:00:00'),
    (10, N'許家豪', N'driver07', N'hashed_pw_010', N'DRIVER', N'0910-010-010', N'hsu@car.com', N'在職', NULL, NULL, '2024-03-01 09:00:00', '2026-03-15 00:00:00'),
    (11, N'楊雅婷', N'driver08', N'hashed_pw_011', N'DRIVER', N'0911-011-011', N'yang@car.com', N'在職', NULL, NULL, '2024-03-10 09:00:00', '2026-03-15 00:00:00'),
    (12, N'羅文彬', N'driver09', N'hashed_pw_012', N'DRIVER', N'0912-012-012', N'lo@car.com', N'在職', NULL, NULL, '2024-04-01 09:00:00', '2026-03-15 00:00:00'),
    (13, N'蔡佩君', N'driver10', N'hashed_pw_013', N'DRIVER', N'0913-013-013', N'tsai@car.com', N'在職', NULL, NULL, '2024-04-15 09:00:00', '2026-03-15 00:00:00'),
    (14, N'劉冠宇', N'staff004', N'hashed_pw_014', N'USER', N'0914-014-014', N'liu@car.com', N'在職', NULL, NULL, '2024-05-01 09:00:00', '2026-03-15 00:00:00'),
    (15, N'謝佳蓉', N'staff005', N'hashed_pw_015', N'USER', N'0915-015-015', N'hsieh@car.com', N'在職', NULL, NULL, '2024-05-15 09:00:00', '2026-03-15 00:00:00'),
    (16, N'廖志明', N'staff006', N'hashed_pw_016', N'USER', N'0916-016-016', N'liao@car.com', N'在職', NULL, NULL, '2024-06-01 09:00:00', '2026-03-15 00:00:00'),
    (17, N'簡淑芬', N'admin002', N'hashed_pw_017', N'ADMIN', N'0917-017-017', N'chien@car.com', N'在職', NULL, NULL, '2024-06-15 09:00:00', '2026-03-15 00:00:00'),
    -- 離職員工（第二筆）
    (18, N'何宗翰', N'staff007', N'hashed_pw_018', N'USER', N'0918-018-018', N'ho@car.com', N'離職', NULL, '2025-12-31', '2024-07-01 09:00:00', '2025-12-31 00:00:00'),
    (19, N'邱怡君', N'staff008', N'hashed_pw_019', N'USER', N'0919-019-019', N'chiu@car.com', N'在職', NULL, NULL, '2024-07-15 09:00:00', '2026-03-15 00:00:00'),
    (20, N'潘柏翰', N'staff009', N'hashed_pw_020', N'USER', N'0920-020-020', N'pan@car.com', N'在職', NULL, NULL, '2024-08-01 09:00:00', '2026-03-15 00:00:00');
SET IDENTITY_INSERT employee OFF;


-- ── driver ───────────────────────────────────────────────────
SET IDENTITY_INSERT driver ON;
INSERT INTO driver
    (driver_id, emp_id, fixed_vehicle_id, license_no, license_expiry, pet_available)
VALUES
    (1, 3, 6, N'A123456789', '2027-12-31 00:00:00', 1),
    (2, 4, 5, N'B234567890', '2027-06-30 00:00:00', 0),
    (3, 5, 1, N'C345678901', '2028-03-31 00:00:00', 1),
    (4, 6, 7, N'D456789012', '2027-09-30 00:00:00', 0),
    (5, 7, 13, N'E567890123', '2028-12-31 00:00:00', 1),
    (6, 8, 14, N'F678901234', '2029-06-30 00:00:00', 0),
    (7, 10, 21, N'G789012345', '2029-12-31 00:00:00', 1),
    (8, 11, 22, N'H890123456', '2029-10-31 00:00:00', 0),
    (9, 12, 23, N'I901234567', '2028-11-30 00:00:00', 1),
    (10, 13, 24, N'J012345678', '2030-01-31 00:00:00', 0);
SET IDENTITY_INSERT driver OFF;

-- ── customer ─────────────────────────────────────────────────
SET IDENTITY_INSERT customer ON;
INSERT INTO customer
    (cust_id, cust_name, cust_phone, cust_email, cust_account, cust_password, cust_address, cust_license, cust_license_expiry, cust_status, current_points, total_accumulated)
VALUES
   --customer (會員表)
--將會員 8 到 20 的初始點數從 (0, 0) 調整為 (50, 150)，以對齊流水帳。
--請將以下內容覆蓋舊檔案的第 885 ~ 904 行

    (1, N'李小龍', N'0911-001-001', N'1rent.eeit215@gmail.com', N'lee001', N'hashed_c_001', N'台北市大安區和平東路1段100號', N'Z123456789', '2028-05-31', N'停權', 1972, 2472),
    (2, N'趙敏', N'0922-002-002', N'chao@mail.com', N'chao002', N'hashed_c_002', N'新竹市東區光復路2段50號', N'A234567890', '2027-08-31', N'啟用', 502, 2802),
    (3, N'孫悟空', N'0933-003-003', N'sun@mail.com', N'sun003', N'hashed_c_003', N'台北市士林區中山北路1號', N'B345678901', '2026-12-31', N'啟用', 450, 750),
    (4, N'唐三藏', N'0944-004-004', N'tang@mail.com', N'tang004', N'hashed_c_004', N'台北市內湖區成功路4段200號', N'C456789012', '2029-01-31', N'停權', 100, 300),
    (5, N'林黛玉', N'0955-005-005', N'daiyu@mail.com', N'daiyu005', N'hashed_c_005', N'新竹市北區北大路1號', N'D567890123', '2027-03-31', N'啟用', 410, 710),
    (6, N'賈寶玉', N'0966-006-006', N'baoyu@mail.com', N'baoyu006', N'hashed_c_006', N'新竹縣竹北市嘉豐南路1號', N'E678901234', '2026-06-30', N'啟用', 741, 741),
    (7, N'許仙', N'0977-007-007', N'xuxian@mail.com', N'xuxian007', N'hashed_c_007', N'台北市文山區指南路100號', N'F789012345', '2029-07-31', N'啟用', 240, 240),
    (8, N'白素貞', N'0978-008-008', N'bai@mail.com', N'bai008', N'hashed_c_008', N'新竹市香山區中華路五段88號', N'G890123456', '2028-08-31', N'啟用', 50, 150),
    (9, N'小青', N'0979-009-009', N'qing@mail.com', N'qing009', N'hashed_c_009', N'台北市松山區南京東路五段1號', N'H901234567', '2027-09-30', N'啟用', 50, 150),
    (10, N'諸葛亮', N'0980-010-010', N'zhuge@mail.com', N'zhuge010', N'hashed_c_010', N'新竹縣竹東鎮中興路1號', N'I012345678', '2029-10-31', N'啟用', 50, 150),
    (11, N'劉備', N'0981-011-011', N'liubei@mail.com', N'liubei011', N'hashed_c_011', N'台北市北投區中央北路1號', N'J123456780', '2028-11-30', N'啟用', 50, 150),
    (12, N'關羽', N'0982-012-012', N'guanyu@mail.com', N'guanyu012', N'hashed_c_012', N'新竹市東區食品路66號', N'K234567801', '2027-12-31', N'啟用', 50, 150),
    (13, N'張飛', N'0983-013-013', N'zhangfei@mail.com', N'zhangfei013', N'hashed_c_013', N'台北市大同區承德路二段2號', N'L345678012', '2029-01-31', N'啟用', 50, 150),
    (14, N'貂蟬', N'0984-014-014', N'diaochan@mail.com', N'diaochan014', N'hashed_c_014', N'新竹縣湖口鄉中山路2段20號', N'M456780123', '2028-02-28', N'啟用', 50, 150),
    (15, N'呂布', N'0985-015-015', N'lubu@mail.com', N'lubu015', N'hashed_c_015', N'台北市萬華區成都路88號', N'N567801234', '2027-03-31', N'停權', 50, 150),
    (16, N'曹操', N'0986-016-016', N'caocao@mail.com', N'caocao016', N'hashed_c_016', N'新竹市北區經國路二段200號', N'O678012345', '2029-04-30', N'啟用', 50, 150),
    (17, N'孫權', N'0987-017-017', N'sunquan@mail.com', N'sunquan017', N'hashed_c_017', N'台北市信義區忠孝東路五段55號', N'P780123456', '2028-05-31', N'啟用', 50, 150),
    (18, N'周瑜', N'0988-018-018', N'zhouyu@mail.com', N'zhouyu018', N'hashed_c_018', N'新竹市東區民生路123號', N'Q801234567', '2027-06-30', N'啟用', 50, 150),
    (19, N'小喬', N'0989-019-019', N'xiaoqiao@mail.com', N'xiaoqiao019', N'hashed_c_019', N'台北市南港區三重路19號', N'R812345678', '2029-07-31', N'啟用', 50, 150),
    (20, N'大喬', N'0990-020-020', N'daqiao@mail.com', N'daqiao020', N'hashed_c_020', N'新竹縣竹北市光明六路80號', N'S823456789', '2028-08-31', N'啟用', 50, 150);

SET IDENTITY_INSERT customer OFF;

-- ── rental_order ─────────────────────────────────────────────
SET IDENTITY_INSERT rental_order ON;
INSERT INTO rental_order
    (order_id, cust_id, vehicle_id, order_type, plan_id, pickup_location_id, return_location_id, pickup_time, return_time, rental_fee, extra_fee, deposit, total_amount, pay_status, order_time, order_status, deposit_pay_method, balance_pay_method, order_remark, invoice_no, contract, updated_at)
VALUES
    (1001, 1, 1, 'SHORT_TERM', 2, 1, 1, '2026-03-05 09:00:00', '2026-03-07 09:00:00', 4000.00, 0.00, 400.00, 4000.00, 'PAID', '2026-02-25 10:00:00', 'CLOSED', 'CREDIT_CARD', 'CREDIT_CARD', N'台北取還', N'INV-20260307001', N'/contracts/1001.pdf', '2026-03-07 10:30:00'),
    (1002, 2, 7, 'SHORT_TERM', 2, 2, 2, '2026-03-08 10:00:00', '2026-03-10 10:00:00', 4000.00, 0.00, 400.00, 4000.00, 'PAID', '2026-03-01 12:00:00', 'CLOSED', 'TRANSFER', 'CREDIT_CARD', N'新竹取還', N'INV-20260310001', N'/contracts/1002.pdf', '2026-03-10 11:00:00'),
    (1003, 3, 2, 'LONG_TERM', 8, 1, 1, '2026-06-01 08:00:00', '2026-09-01 08:00:00', 75000.00, 0.00, 7500.00, 75000.00, 'DEPOSIT_PAID', '2026-05-15 09:00:00', 'RESERVED', 'CREDIT_CARD', NULL, N'5/29 後未來長租預約；未到出發時間不得轉出租中', NULL, N'/contracts/1003.pdf', '2026-05-15 09:10:00'),
    (1004, 4, 1, 'SHORT_TERM', 2, 1, 1, '2026-05-29 09:00:00', '2026-05-30 09:00:00', 2000.00, 0.00, 200.00, 2000.00, 'UNPAID', '2026-05-15 09:00:00', 'RESERVED', NULL, NULL, N'5/29 後未來短租預約', NULL, NULL, '2026-05-15 17:00:00'),
    (1005, 5, 5, 'SHORT_TERM', 5, 1, 1, '2026-02-28 07:00:00', '2026-03-01 07:00:00', 3500.00, 0.00, 350.00, 3500.00, 'REFUNDED', '2026-02-20 09:00:00', 'CANCELLED', 'CREDIT_CARD', NULL, N'客戶臨時取消，未取車', NULL, NULL, '2026-02-25 08:00:00'),
    (1006, 1, 3, 'LONG_TERM', 6, 1, 1, '2025-12-01 09:00:00', '2026-03-01 09:00:00', 54000.00, 0.00, 5400.00, 54000.00, 'PAID', '2025-11-20 09:00:00', 'CLOSED', 'TRANSFER', 'CASH', N'長租期滿正常還車', N'INV-20260301001', N'/contracts/1006.pdf', '2026-03-01 11:00:00'),
    (1007, 2, 6, 'SHORT_TERM', 3, 1, 1, '2026-05-30 09:00:00', '2026-05-31 09:00:00', 2500.00, 0.00, 250.00, 2500.00, 'DEPOSIT_PAID', '2026-05-15 09:00:00', 'RESERVED', 'CREDIT_CARD', NULL, N'5/29 後未來短租預約', NULL, NULL, '2026-05-15 18:30:00'),
    (1008, 3, 1, 'LONG_TERM', 7, 1, 1, '2026-06-03 10:00:00', '2026-12-03 10:00:00', 126000.00, 0.00, 12600.00, 126000.00, 'UNPAID', '2026-05-15 09:00:00', 'RESERVED', NULL, NULL, N'5/29 後企業長租預約', NULL, NULL, '2026-05-15 15:45:00'),
    (1009, 4, 3, 'SHORT_TERM', 1, 1, 1, '2026-03-14 14:00:00', '2026-03-16 14:00:00', 3000.00, 0.00, 300.00, 3000.00, 'REFUNDED', '2026-03-10 09:00:00', 'CANCELLED', 'CREDIT_CARD', NULL, N'歷史取消單，避免 5/15 後仍有租賃中狀態', NULL, NULL, '2026-03-14 14:10:00'),
    (1010, 5, 7, 'SHORT_TERM', 2, 2, 2, '2026-03-12 09:00:00', '2026-03-14 18:00:00', 6000.00, 0.00, 600.00, 6000.00, 'REFUNDED', '2026-03-05 09:00:00', 'CANCELLED', 'TRANSFER', NULL, N'歷史取消單，移除 OVERDUE 測試狀態', NULL, NULL, '2026-03-15 08:30:00'),
    (1011, 6, 15, 'SHORT_TERM', 2, 1, 1, '2026-03-13 08:00:00', '2026-03-15 08:00:00', 4000.00, 0.00, 400.00, 4000.00, 'REFUNDED', '2026-03-08 09:00:00', 'CANCELLED', 'CREDIT_CARD', NULL, N'歷史取消單，移除 RETURNED 測試狀態', NULL, NULL, '2026-03-15 08:40:00'),
    (1012, 6, 13, 'SHORT_TERM', 1, 1, 1, '2026-06-05 09:00:00', '2026-06-06 09:00:00', 1500.00, 0.00, 150.00, 1500.00, 'DEPOSIT_PAID', '2026-05-15 09:00:00', 'RESERVED', 'CREDIT_CARD', NULL, N'5/29 後未來短租預約', NULL, NULL, '2026-05-15 14:00:00'),
    (1013, 7, 21, 'SHORT_TERM', 1, 1, 2, '2026-03-02 09:00:00', '2026-03-03 09:00:00', 1500.00, 500.00, 200.00, 2000.00, 'PAID', '2026-02-25 09:00:00', 'CLOSED', 'CREDIT_CARD', 'CASH', N'台北取車新竹還車', N'INV-20260303001', N'/contracts/1013.pdf', '2026-03-03 10:00:00'),
    (1014, 8, 25, 'SHORT_TERM', 3, 1, 1, '2026-06-06 09:00:00', '2026-06-08 09:00:00', 5000.00, 0.00, 500.00, 5000.00, 'DEPOSIT_PAID', '2026-05-15 09:00:00', 'RESERVED', 'CREDIT_CARD', NULL, N'5/29 後休旅車短租預約', NULL, N'/contracts/1014.pdf', '2026-05-15 09:10:00'),
    (1015, 9, 26, 'SHORT_TERM', 2, 1, 1, '2026-03-13 08:00:00', '2026-03-15 07:00:00', 4000.00, 0.00, 400.00, 4000.00, 'REFUNDED', '2026-03-09 10:00:00', 'CANCELLED', 'TRANSFER', NULL, N'歷史取消單，移除 RETURNED 測試狀態', NULL, NULL, '2026-03-15 07:40:00'),
    (1016, 10, 28, 'SHORT_TERM', 2, 1, 1, '2026-06-08 09:00:00', '2026-06-09 09:00:00', 2000.00, 0.00, 200.00, 2000.00, 'UNPAID', '2026-05-15 09:00:00', 'RESERVED', NULL, NULL, N'5/29 後未來短租預約', NULL, NULL, '2026-05-15 09:00:00'),
    (1017, 11, 29, 'SHORT_TERM', 4, 2, 2, '2026-04-01 09:00:00', '2026-04-02 09:00:00', 3000.00, 0.00, 300.00, 3000.00, 'REFUNDED', '2026-03-10 11:00:00', 'CANCELLED', 'CREDIT_CARD', NULL, N'已取消並退款完成', NULL, NULL, '2026-03-11 14:00:00'),
    (1018, 12, 30, 'LONG_TERM', 8, 2, 2, '2026-06-01 10:00:00', '2026-12-01 10:00:00', 150000.00, 0.00, 15000.00, 150000.00, 'UNPAID', '2026-03-15 10:00:00', 'RESERVED', NULL, NULL, N'六月長租預約', NULL, NULL, '2026-03-15 10:00:00'),
    (1019, 13, 30, 'SHORT_TERM', 3, 2, 2, '2026-03-12 08:00:00', '2026-03-14 18:00:00', 7500.00, 0.00, 750.00, 7500.00, 'REFUNDED', '2026-03-08 09:00:00', 'CANCELLED', 'TRANSFER', NULL, N'歷史取消單，移除 OVERDUE 測試狀態', NULL, NULL, '2026-03-15 08:00:00'),
    (1020, 14, 21, 'SHORT_TERM', 1, 2, 1, '2026-06-10 09:00:00', '2026-06-11 09:00:00', 1500.00, 500.00, 200.00, 2000.00, 'DEPOSIT_PAID', '2026-05-15 13:00:00', 'RESERVED', 'MOBILE_PAY', NULL, N'5/29 後跨站還車預約', NULL, NULL, '2026-05-15 13:00:00');
SET IDENTITY_INSERT rental_order OFF;

-- ── short_term_details ───────────────────────────────────────
SET IDENTITY_INSERT short_term_details ON;
INSERT INTO short_term_details
    (detail_id, order_id, actual_pickup_time, actual_return_time, start_mileage, end_mileage, note_desc, fuel_level_return, created_at, updated_at)
VALUES
    (1, 1001, '2026-03-05 09:10:00', '2026-03-07 09:25:00', 30000, 30500, N'CLOSED 已補實際還車與里程', 'FULL', '2026-02-25 10:00:00', '2026-03-07 09:25:00'),
    (2, 1002, '2026-03-08 10:05:00', '2026-03-10 10:30:00', 45000, 45280, N'CLOSED 已補實際還車與里程', 'FULL', '2026-03-01 12:00:00', '2026-03-10 10:30:00'),
    (3, 1004, NULL, NULL, NULL, NULL, N'5/29 後未來預約，尚未取車', NULL, '2026-05-15 09:00:00', '2026-05-15 17:00:00'),
    (4, 1005, NULL, NULL, NULL, NULL, N'取消訂單，未取車', NULL, '2026-02-20 09:00:00', '2026-02-25 08:00:00'),
    (5, 1007, NULL, NULL, NULL, NULL, N'5/29 後未來預約，尚未取車', NULL, '2026-05-15 09:00:00', '2026-05-15 18:30:00'),
    (6, 1009, NULL, NULL, NULL, NULL, N'取消訂單，未取車', NULL, '2026-03-10 09:00:00', '2026-03-14 14:10:00'),
    (7, 1010, NULL, NULL, NULL, NULL, N'取消訂單，未取車', NULL, '2026-03-05 09:00:00', '2026-03-15 08:30:00'),
    (8, 1011, NULL, NULL, NULL, NULL, N'取消訂單，未取車', NULL, '2026-03-08 09:00:00', '2026-03-15 08:40:00'),
    (9, 1013, '2026-03-02 09:10:00', '2026-03-03 09:20:00', 15000, 15200, N'CLOSED 已補實際還車與里程', 'FULL', '2026-02-25 09:00:00', '2026-03-03 09:20:00'),
    (10, 1014, NULL, NULL, NULL, NULL, N'5/29 後未來預約，尚未取車', NULL, '2026-05-15 09:00:00', '2026-05-15 09:10:00'),
    (11, 1015, NULL, NULL, NULL, NULL, N'取消訂單，未取車', NULL, '2026-03-09 10:00:00', '2026-03-15 07:40:00'),
    (12, 1016, NULL, NULL, NULL, NULL, N'5/29 後未來預約，尚未取車', NULL, '2026-05-15 09:00:00', '2026-05-15 09:00:00'),
    (13, 1017, NULL, NULL, NULL, NULL, N'取消訂單，未取車', NULL, '2026-03-10 11:00:00', '2026-03-11 14:00:00'),
    (14, 1019, NULL, NULL, NULL, NULL, N'取消訂單，未取車', NULL, '2026-03-08 09:00:00', '2026-03-15 08:00:00'),
    (15, 1020, NULL, NULL, NULL, NULL, N'5/29 後未來跨站還車預約', NULL, '2026-05-15 13:00:00', '2026-05-15 13:00:00');
SET IDENTITY_INSERT short_term_details OFF;

-- ── long_term_details ────────────────────────────────────────
SET IDENTITY_INSERT long_term_details ON;
INSERT INTO long_term_details
    (detail_id, order_id, actual_pickup_time, actual_return_time, contract_months, monthly_payment, billing_day, paid_months, delivery_address, start_mileage, end_mileage, note_desc, created_at, updated_at)
VALUES
    (1, 1003, NULL, NULL, 3, 25000.00, 1, 0, N'台北市信義區松仁路100號', NULL, NULL, N'5/29 後未來長租預約，尚未交車', '2026-05-15 09:00:00', '2026-05-15 09:10:00'),
    (2, 1006, '2025-12-01 09:20:00', '2026-03-01 10:20:00', 3, 18000.00, 1, 3, N'台北市中正區忠孝西路1號', 50000, 53100, N'CLOSED 已補實際還車與里程', '2025-11-20 09:00:00', '2026-03-01 10:20:00'),
    (3, 1008, NULL, NULL, 6, 21000.00, 5, 0, N'新竹市科學園區路10號', NULL, NULL, N'5/29 後未來長租預約，尚未交車', '2026-05-15 09:00:00', '2026-05-15 15:45:00'),
    (4, 1018, NULL, NULL, 6, 25000.00, 1, 0, N'新竹市東區關新路88號', NULL, NULL, N'未來長租預約，尚未交車', '2026-03-15 10:00:00', '2026-03-15 10:00:00');
SET IDENTITY_INSERT long_term_details OFF;

---------------------下方蓋掉原檔案的964行到1070行--------------------------------


-- ============================================================
-- 【功能3假資料】商品、兌換訂單、voucher、點數紀錄
-- ============================================================

-- ── product ───────────────────────────────────────────────────

SET IDENTITY_INSERT product ON;
INSERT INTO product
    (product_id, product_name, description, points_required, stock_quantity, is_active, image_url, category)
VALUES
    --product (商品表)
--替換點數商品的圖片網址為有效網址，修復前端圖片顯示。
--請將以下內容覆蓋舊檔案的第 980 ~ 1003 行

  -- 汽車配件
    (1, N'記憶棉腰靠', N'符合人體工學設計，長途開車也舒適', 300, 98, 1, N'https://cdn-next.cybassets.com/media/W1siZiIsIjMyMzQ0L3Byb2R1Y3RzLzU0OTYyOTY3LzE3NDczNzM3MzlfODYxZWVkNzllOTRlNTJmZmNiODMuanBlZyJdLFsicCIsInRodW1iIiwiNjAweDYwMCJdXQ.jpeg?sha=86ae16779861632e', N'汽車配件'),
    (2, N'高效能合成機油', N'全合成機油，保護引擎延長壽命', 500, 99, 1, N'https://pgo.com.tw/wp-content/uploads/10W40-%E9%AB%98%E6%95%88%E8%83%BD%E5%85%A8%E5%90%88%E6%88%90%E5%9B%9B%E8%A1%8C%E7%A8%8B%E6%A9%9F%E6%B2%B9.jpg?w=600&h=400&fit=crop', N'汽車配件'),
    (3, N'車用負離子空氣清淨機', N'有效去除異味，讓車內空氣清新', 1800, 49, 1, N'https://cdn1.cybassets.com/media/W1siZiIsIjI0MDcyL3Byb2R1Y3RzLzQyNjE2MDA1LzE2OTcyNjg5NjlfYjNjZTE2NTMwZjE1OWQzYWRlZjkuanBlZyJdLFsicCIsInRodW1iIiwiNjAweDYwMCJdXQ.jpeg?sha=153dcde8aba8bf62', N'汽車配件'),
    (4, N'車用吸塵器', N'輕巧好握，深入清潔每個角落', 2200, 20, 1, N'https://shoplineimg.com/5a73cf3c6ef2d4344f001d2d/62d9126f1b1bf136338272f3/800x.jpg??w=600&h=400&fit=crop', N'汽車配件'),
    (5, N'胎壓偵測器', N'即時監控四輪胎壓，行車更安全', 3500, 0, 1, N'https://www.taiwanexcellence.org/upload/product/old/old2022/112245IB-C112_L.jpg?w=600&h=400&fit=crop', N'汽車配件'),
    (6, N'行車紀錄器', N'1080P高畫質錄影，雙鏡頭前後同錄', 5000, 200, 0, N'https://iroad.tw/cdn/shop/files/pdt-x30_pic.png?v=1723299626?w=600&h=400&fit=crop', N'汽車配件'),
    -- 加值服務
    (7, N'免費洗車券', N'全車內外清洗，讓您的車煥然一新', 200, 49, 1, N'https://images.contentstack.io/v3/assets/blt62d40591b3650da3/bltee515d46b1b13f83/658ee585d082f768b425faf9/hero_PN1305_HowOftenWashCar_Header-1.jpg?w=600&h=400&fit=crop', N'加值服務'),
    (8, N'兒童安全座椅升級券', N'租車期間免費升級兒童安全座椅一次', 300, 29, 1, N'https://img.carstuff.com.tw/images/stories/2022/Jason/04/07/Volvo/1.jpg?w=600&h=400&fit=crop', N'加值服務'),
    (9, N'GPS導航一日免費券', N'租車期間免費附贈GPS導航一日', 150, 80, 1, N'https://p3.8891.com.tw/nc/newcar/article/2017/09/11/1505083506852618_900_600.jpg?w=600&h=400&fit=crop', N'加值服務'),
    (10, N'道路救援加強券', N'24小時道路救援保障，行車無後顧之憂', 400, 40, 0, N'https://bank.sinopac.com/upload/sinopac/picture/15fe83c12990000050db.jpg?w=600&h=400&fit=crop', N'加值服務'),
    (11, N'免費代駕券', N'指定地區免費代駕服務一次', 800, 0, 1, N'https://pgw.udn.com.tw/gw/photo.php?u=https://uc.udn.com.tw/photo/2024/12/03/0/31020751.jpg&x=0&y=0&sw=0&sh=0&sl=W&fw=800&exp=3600&nt=1?w=600&h=400&fit=crop', N'加值服務'),
    -- 禮品兌換
    (12, N'星巴克電子禮品卡100元', N'可於全台星巴克門市使用', 500, 60, 1, N'https://www.starbucks.com.tw/coffee/objects/images/catalog/banner_coldbrew.jpg?w=600&h=400&fit=crop', N'禮品兌換'),
    (13, N'星巴克電子禮品卡150元', N'可於全台星巴克門市使用', 750, 60, 1, N'https://assets.juksy.com/files/articles/142707/69b3c464f18ef.jpg?w=600&h=400&fit=crop', N'禮品兌換'),
    (14, N'7-11電子禮品卡100元', N'可於全台7-11門市使用', 500, 80, 1, N'https://media-asset-p-asse.ticketxpress.com/txc-tw/prod/media-library/b72f130e-7c90-4e46-b657-54d2484106a8.jpg?w=600&h=400&fit=crop', N'禮品兌換'),
    (15, N'全家電子禮品卡100元', N'可於全台全家門市使用', 500, 80, 1, N'https://dwfavn5d0m4r1.cloudfront.net/App_Images/750/ChannelProduct/20241022/e7af8e12-2cdd-484c-9f5b-bc916f965503.jpg?w=600&h=400&fit=crop', N'禮品兌換'),
    (16, N'誠品書店禮品卡200元', N'可於全台誠品書店門市使用', 1000, 0, 1, N'https://www.travel.taipei/content/images/attractions/573438/1024x768_attractions-image-7n32bingyew6footkwrilq.jpg?w=600&h=400&fit=crop', N'禮品兌換'),
    -- 合作品牌
    (17, N'全家便利商店100點', N'兌換全家便利商店會員點數100點', 300, 100, 1, N'https://edge.aif.tw/content/images/2022/01/shutterstock_1164937858.jpg?w=600&h=400&fit=crop', N'合作品牌'),
    (18, N'中油加油金100元', N'可於中油加油站折抵100元加油費', 600, 50, 1, N'https://uc.udn.com.tw/photo/2020/03/27/realtime/7653901.jpg?w=600&h=400&fit=crop', N'合作品牌'),
    (19, N'麥當勞大麥克套餐券', N'可兌換麥當勞大麥克餐一份', 400, 40, 0, N'https://dream.bhuntr.com/ghost/images/2024/03/---2024-03-01---12.45.05.png?w=600&h=400&fit=crop', N'合作品牌'),
    (20, N'momo購物金200元', N'可於momo購物網折抵200元', 1200, 30, 1, N'https://image-cdn.learnin.tw/bnextmedia/image/album/2024-09/img-1725369594-25625.jpg?w=600&output=webp?w=600&h=400&fit=crop', N'合作品牌');

SET IDENTITY_INSERT product OFF;


-- ── redemption_orders ─────────────────────────────────────────
-- 原有 6 筆（保留不動）
-- 新增 19 筆（cust 1-20 各一筆 ACTIVE，cust 4 停權跳過，共 19 筆）
-- redemption_id 規劃：7=cust1, 8=cust2, 9=cust3, 10=cust5, 11=cust6,
--   12=cust7, 13=cust8, 14=cust9, 15=cust10, 16=cust11, 17=cust12,
--   18=cust13, 19=cust14, 20=cust15, 21=cust16, 22=cust17,
--   23=cust18, 24=cust19, 25=cust20
SET IDENTITY_INSERT redemption_orders ON;
INSERT INTO redemption_orders
    (redemption_id, cust_id, product_id, product_quantity, points_spent, order_status, create_time, update_time)
VALUES
    -- ── 原有 6 筆（不動）──
    (1, 1, 1, 1, 300, N'USED', '2026-01-15 10:00:00', '2026-01-20 14:00:00'),
    (2, 2, 3, 1, 1800, N'USED', '2026-02-01 11:00:00', '2026-02-05 10:00:00'),
    (3, 2, 2, 1, 500, N'ACTIVE', '2026-03-01 09:00:00', NULL),
    (4, 1, 7, 1, 200, N'USED', '2026-03-05 14:00:00', '2026-03-10 11:00:00'),
    (5, 3, 8, 1, 300, N'EXPIRED', '2026-03-10 16:00:00', '2026-03-15 00:00:00'),
    (6, 5, 1, 1, 300, N'EXPIRED', '2026-03-12 08:30:00', '2026-03-15 09:00:00'),
    -- ── 新增：每位會員一筆 ACTIVE 訂單，對應15天內到期 UNUSED 票券 ──
    -- 使用 product_id=9（GPS導航一日免費券，150點），點數消耗最低
    -- cust 1、2 點數充足改用 product_id=7（免費洗車券，200點）
    (7, 1, 7, 1, 200, N'ACTIVE', '2026-05-10 10:00:00', NULL),
    -- 李小龍
    (8, 2, 9, 1, 150, N'ACTIVE', '2026-05-11 10:00:00', NULL),
    -- 趙敏
    (9, 3, 9, 1, 150, N'ACTIVE', '2026-05-12 09:00:00', NULL),
    -- 孫悟空
    -- cust 4 唐三藏停權，不補
    (10, 5, 9, 1, 150, N'ACTIVE', '2026-05-10 11:00:00', NULL),
    -- 林黛玉
    (11, 6, 9, 1, 150, N'ACTIVE', '2026-05-11 14:00:00', NULL),
    -- 賈寶玉
    (12, 7, 9, 1, 150, N'ACTIVE', '2026-05-10 15:00:00', NULL),
    -- 許仙（WB已到期，帳面0點，補充說明見points_history）
    (13, 8, 9, 1, 150, N'ACTIVE', '2026-05-12 10:00:00', NULL),
    -- 白素貞
    (14, 9, 9, 1, 150, N'ACTIVE', '2026-05-13 09:00:00', NULL),
    -- 小青
    (15, 10, 9, 1, 150, N'ACTIVE', '2026-05-10 13:00:00', NULL),
    -- 諸葛亮
    (16, 11, 9, 1, 150, N'ACTIVE', '2026-05-11 11:00:00', NULL),
    -- 劉備
    (17, 12, 9, 1, 150, N'ACTIVE', '2026-05-12 14:00:00', NULL),
    -- 關羽
    (18, 13, 9, 1, 150, N'ACTIVE', '2026-05-13 10:00:00', NULL),
    -- 張飛
    (19, 14, 9, 1, 150, N'ACTIVE', '2026-05-10 16:00:00', NULL),
    -- 貂蟬
    (20, 15, 9, 1, 150, N'ACTIVE', '2026-05-11 15:00:00', NULL),
    -- 呂布（停權但保留測試用）
    (21, 16, 9, 1, 150, N'ACTIVE', '2026-05-12 11:00:00', NULL),
    -- 曹操
    (22, 17, 9, 1, 150, N'ACTIVE', '2026-05-13 14:00:00', NULL),
    -- 孫權
    (23, 18, 9, 1, 150, N'ACTIVE', '2026-05-10 09:00:00', NULL),
    -- 周瑜
    (24, 19, 9, 1, 150, N'ACTIVE', '2026-05-11 16:00:00', NULL),
    -- 小喬
    (25, 20, 9, 1, 150, N'ACTIVE', '2026-05-12 15:00:00', NULL);
-- 大喬
SET IDENTITY_INSERT redemption_orders OFF;

-- ── voucher ───────────────────────────────────────────────────
-- 原有 6 筆（保留不動）
-- 新增 19 筆 UNUSED 票券（expiry_date 分散於 2026-05-21 ~ 2026-06-04，均在15天內）
SET IDENTITY_INSERT voucher ON;
INSERT INTO voucher
    (voucher_id, redemption_id, voucher_code, status, use_time, expiry_date)
VALUES
    -- ── 原有 6 筆（不動）──
    (1, 1, N'VC-001-ABCD1234', N'USED', '2026-01-20 14:00:00', '2027-01-15 10:00:00'),
    (2, 2, N'VC-002-EFGH5678', N'USED', '2026-02-05 10:00:00', '2027-02-01 11:00:00'),
    (3, 3, N'VC-003-IJKL9012', N'UNUSED', NULL, '2027-03-01 09:00:00'),
    (4, 4, N'VC-004-MNOP3456', N'USED', '2026-03-10 11:00:00', '2027-03-05 14:00:00'),
    (5, 5, N'VC-005-QRST7890', N'EXPIRED', NULL, '2026-03-14 23:59:59'),
    (6, 6, N'VC-006-UVWX1234', N'EXPIRED', NULL, '2026-03-14 23:59:59'),
    -- ── 新增：15天內到期 UNUSED 票券 ──
    -- expiry_date 分散於 2026-05-21 ~ 2026-06-04
    (7, 7, N'VC-007-A1B2C3D4', N'UNUSED', NULL, '2026-05-21 10:00:00'),
    -- cust 1 李小龍
    (8, 8, N'VC-008-E5F6G7H8', N'UNUSED', NULL, '2026-05-22 10:00:00'),
    -- cust 2 趙敏
    (9, 9, N'VC-009-I9J0K1L2', N'UNUSED', NULL, '2026-05-23 09:00:00'),
    -- cust 3 孫悟空
    (10, 10, N'VC-010-M3N4O5P6', N'UNUSED', NULL, '2026-05-24 11:00:00'),
    -- cust 5 林黛玉
    (11, 11, N'VC-011-Q7R8S9T0', N'UNUSED', NULL, '2026-05-25 14:00:00'),
    -- cust 6 賈寶玉
    (12, 12, N'VC-012-U1V2W3X4', N'UNUSED', NULL, '2026-05-26 15:00:00'),
    -- cust 7 許仙
    (13, 13, N'VC-013-Y5Z6A7B8', N'UNUSED', NULL, '2026-05-27 10:00:00'),
    -- cust 8 白素貞
    (14, 14, N'VC-014-C9D0E1F2', N'UNUSED', NULL, '2026-05-28 09:00:00'),
    -- cust 9 小青
    (15, 15, N'VC-015-G3H4I5J6', N'UNUSED', NULL, '2026-05-29 13:00:00'),
    -- cust10 諸葛亮
    (16, 16, N'VC-016-K7L8M9N0', N'UNUSED', NULL, '2026-05-30 11:00:00'),
    -- cust11 劉備
    (17, 17, N'VC-017-O1P2Q3R4', N'UNUSED', NULL, '2026-05-31 14:00:00'),
    -- cust12 關羽
    (18, 18, N'VC-018-S5T6U7V8', N'UNUSED', NULL, '2026-06-01 10:00:00'),
    -- cust13 張飛
    (19, 19, N'VC-019-W9X0Y1Z2', N'UNUSED', NULL, '2026-06-01 16:00:00'),
    -- cust14 貂蟬
    (20, 20, N'VC-020-A3B4C5D6', N'UNUSED', NULL, '2026-06-02 15:00:00'),
    -- cust15 呂布
    (21, 21, N'VC-021-E7F8G9H0', N'UNUSED', NULL, '2026-06-02 11:00:00'),
    -- cust16 曹操
    (22, 22, N'VC-022-I1J2K3L4', N'UNUSED', NULL, '2026-06-03 14:00:00'),
    -- cust17 孫權
    (23, 23, N'VC-023-M5N6O7P8', N'UNUSED', NULL, '2026-06-03 09:00:00'),
    -- cust18 周瑜
    (24, 24, N'VC-024-Q9R0S1T2', N'UNUSED', NULL, '2026-06-04 16:00:00'),
    -- cust19 小喬
    (25, 25, N'VC-025-U3V4W5X6', N'UNUSED', NULL, '2026-06-04 15:00:00');
-- cust20 大喬
SET IDENTITY_INSERT voucher OFF;

-- ── points_history ────────────────────────────────────────────
-- 【重製說明】
-- 新增欄位：available_points INT NULL（FIFO 扣點用）
-- 規則：
--   RENTAL    = floor(total_amount / 50)
--   TRANSFER  = floor(total_amount / 33)
--   WELCOME_BONUS = 固定 100 點（每帳號一次）
--   FIRST_RENTAL  = 固定 50 點（首次 CLOSED 租車，每帳號一次）
--   BIRTHDAY      = 固定 50 點（每年一次）
-- available_points：
--   獲得點數類型（正向）→ 初始值 = pointsChange，被FIFO扣後遞減
--   消耗/過期類型（負向）→ NULL
-- 完成租車訂單（CLOSED）：
--   1001(cust1,4000元→80點) 1002(cust2,4000元→80點)
--   1006(cust1,54000元→1080點) 1013(cust7,2000元→40點)
-- 完成專車訂單（已完成）：
--   transfer2(cust1,3750元→floor(3750/33)=113點)
--   transfer3(cust2,750元→floor(750/33)=22點)
--   transfer6(cust6,19700元→floor(19700/33)=597點)
-- FIRST_RENTAL：cust1(1006最早CLOSED)、cust2(1002)、cust7(1013)
-- ──────────────────────────────────────────────────────────────
-- cust 1 李小龍：
--   收入：WB100(已過期)+FR50(已過期)+RENTAL1080+RENTAL80+TRANSFER113+BIRTHDAY1000(特殊加碼)=2323
--   支出：REDEMPTION300+REDEMPTION200=500
--   帳面：2323-500=1823 → 但 customer.current_points=1772，差51
--   調整：TRANSFER 用 112（原始假資料值，保持帳面一致）
--   重算：1080+80+112+1000=2272=total_accumulated ✓，2272-500=1772=current_points ✓
--   FIFO：BIRTHDAY1000最舊→扣300(order1)+200(order4)=500，avail=500
-- cust 2 趙敏：
--   收入：WB100(已過期)+FR50(已過期)+BIRTHDAY2500(特殊加碼)+TRANSFER22+RENTAL80
--   支出：REDEMPTION1800+REDEMPTION500=2300
--   帳面：2500+22+80=2602=total_accumulated ✓，2602-2300=302=current_points ✓
--   FIFO：BIRTHDAY2500最舊→扣1800+500=2300，avail=200
-- cust 3 孫悟空：
--   收入：WB100(已過期)+BIRTHDAY600(特殊加碼)
--   支出：REDEMPTION300（order5 EXPIRED，點數仍扣）
--   帳面：600=total_accumulated ✓，600-300=300=current_points ✓
--   FIFO：BIRTHDAY600扣300，avail=300
-- cust 4 唐三藏（停權）：
--   收入：WB100(已過期)+RENTAL200
--   支出：EXPIRED200
--   帳面：200=total_accumulated ✓，0=current_points ✓
-- cust 5 林黛玉：
--   收入：WB100(已過期)+BIRTHDAY500+BIRTHDAY60
--   支出：REDEMPTION300（order6 EXPIRED）
--   帳面：500+60=560=total_accumulated ✓，560-300=260=current_points ✓
--   FIFO：BIRTHDAY500先扣300，avail=200；BIRTHDAY60未動，avail=60
-- cust 6 賈寶玉：
--   收入：WB100(已過期)+TRANSFER591（原始值保留）
--   帳面：591=total_accumulated ✓，591=current_points ✓
-- cust 7 許仙：
--   收入：WB100(已過期)+FR50(已過期)+RENTAL40
--   帳面：40=total_accumulated ✓，40=current_points ✓
-- cust 8-20：WB100(各已過期)，current=0，total=0
--   ⚠️ 注意：cust 8-20 各補一筆即將到期的 BIRTHDAY 50點（供排程測試）
--   這筆50點加入後 current_points 實際應為 50，
--   但 customer.current_points 維持 0（需組長確認是否同步更新）
-- ──────────────────────────────────────────────────────────────
SET IDENTITY_INSERT points_history ON;
INSERT INTO points_history
    (record_id, cust_id, remain_points, change_type, points_change, reference_id, notes, create_time, expire_time, available_points)
VALUES
  --points_history (點數歷史)
--將會員 8 到 20 的註冊、生日贈點時間從 2026 挪至 2025，以符合系統 Present Day 測試時間線，並對齊點數扣除邏輯。
--請將以下內容覆蓋舊檔案的第 1187 ~ 1336 行

    -- ══ cust 1 李小龍 ══
    -- WELCOME_BONUS +100 
    (1, 1, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2025-12-01 00:00:00', '2026-12-01 00:00:00', 100),
    -- FIRST_RENTAL +50 
    (3, 1, 1930, N'FIRST_RENTAL', 50, N'1006', N'首次租車獎勵', '2026-03-01 11:10:00', '2027-03-01 11:10:00', 50),
    -- BIRTHDAY +1000（特殊活動加碼，維持原始帳面）
    (5, 1, 1100, N'BIRTHDAY', 1000, NULL, N'年度活動贈點（特殊加碼）', '2026-01-01 00:00:00', '2027-01-01 00:00:00', 500),
    -- REDEMPTION -300（order1，FIFO從BIRTHDAY1000扣）
    (6, 1, 800, N'REDEMPTION', -300, N'1', N'兌換：記憶棉腰靠', '2026-01-15 10:00:00', '2027-01-15 10:00:00', NULL),
    -- REDEMPTION -200（order4，FIFO繼續從BIRTHDAY1000扣，avail=500）
    (7, 1, 1730, N'REDEMPTION', -200, N'4', N'兌換：免費洗車券', '2026-03-05 14:00:00', '2027-03-05 14:00:00', NULL),
    -- RENTAL +1080（order1006，54000/50=1080）
    (8, 1, 1880, N'RENTAL', 1080, N'1006', N'長租結案獲得點數', '2026-03-01 11:00:00', '2027-03-01 11:00:00', 1080),
    -- RENTAL +80（order1001，4000/50=80）
    (9, 1, 1810, N'RENTAL', 80, N'1001', N'租車結案獲得點數', '2026-03-07 10:30:00', '2027-03-07 10:30:00', 80),
    -- TRANSFER +112（transfer2，3750/33=113，保持原始112維持帳面）
    (10, 1, 1922, N'TRANSFER', 112, N'2', N'專車接送完成獲得點數', '2026-03-12 11:00:00', '2027-03-12 11:00:00', 112),
    -- 即將到期點數（15天內，供排程測試）
    (11, 1, 1972, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-05-28 00:00:00', '2026-05-28 00:00:00', 50),

    -- ══ cust 2 趙敏 ══
    -- WELCOME_BONUS +100
    (12, 2, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2025-11-01 00:00:00', '2026-11-01 00:00:00', 100),
    -- FIRST_RENTAL +50
    (14, 2, 350, N'FIRST_RENTAL', 50, N'1002', N'首次租車獎勵', '2026-03-10 11:10:00', '2027-03-10 11:10:00', 50),
    -- BIRTHDAY +2500（特殊活動加碼）
    (16, 2, 2600, N'BIRTHDAY', 2500, NULL, N'年度活動贈點（特殊加碼）', '2026-01-01 00:00:00', '2027-01-01 00:00:00', 200),
    -- REDEMPTION -1800（order2，FIFO從BIRTHDAY2500扣）
    (17, 2, 800, N'REDEMPTION', -1800, N'2', N'兌換：車用負離子空氣清淨機', '2026-02-01 11:00:00', '2027-02-01 11:00:00', NULL),
    -- REDEMPTION -500（order3，FIFO繼續從BIRTHDAY2500扣，avail=200）
    (18, 2, 300, N'REDEMPTION', -500, N'3', N'兌換：高效能合成機油', '2026-03-01 09:00:00', '2027-03-01 09:00:00', NULL),
    -- TRANSFER +22（transfer3，750/33=22）
    (19, 2, 452, N'TRANSFER', 22, N'3', N'專車接送完成獲得點數', '2026-03-11 10:20:00', '2027-03-11 10:20:00', 22),
    -- RENTAL +80（order1002，4000/50=80）
    (20, 2, 430, N'RENTAL', 80, N'1002', N'租車結案獲得點數', '2026-03-10 11:00:00', '2027-03-10 11:00:00', 80),
    -- 即將到期點數（15天內）
    (21, 2, 502, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-05-29 00:00:00', '2026-05-29 00:00:00', 50),

    -- ══ cust 3 孫悟空 ══
    -- WELCOME_BONUS +100
    (22, 3, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2025-10-01 00:00:00', '2026-10-01 00:00:00', 100),
    -- BIRTHDAY +600（特殊活動加碼）
    (24, 3, 700, N'BIRTHDAY', 600, NULL, N'年度活動贈點（特殊加碼）', '2026-02-20 00:00:00', '2027-02-20 00:00:00', 300),
    -- REDEMPTION -300（order5 EXPIRED，FIFO從BIRTHDAY600扣，avail=300）
    (25, 3, 400, N'REDEMPTION', -300, N'5', N'兌換：兒童安全座椅升級券', '2026-03-10 16:00:00', '2027-03-10 16:00:00', NULL),
    -- 即將到期點數（15天內）
    (26, 3, 450, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-05-30 00:00:00', '2026-05-30 00:00:00', 50),

    -- ══ cust 4 唐三藏（停權）══
    -- WELCOME_BONUS +100 
    (27, 4, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2025-09-01 00:00:00', '2026-09-01 00:00:00', 100),
    -- RENTAL +200（歷史訂單 OLD-ORDER-004）
    (29, 4, 300, N'RENTAL', 200, N'OLD-ORDER-004', N'歷史租車獲得點數', '2026-01-10 10:00:00', '2027-01-10 10:00:00', 0),
    -- EXPIRED -200
    (30, 4, 100, N'EXPIRED', -200, NULL, N'年度點數到期扣除', '2026-03-01 00:00:00', '2027-03-01 00:00:00', NULL),

    -- ══ cust 5 林黛玉 ══
    -- WELCOME_BONUS +100 已過期
    (31, 5, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2025-08-01 00:00:00', '2026-08-01 00:00:00', 100),
    -- BIRTHDAY +500
    (33, 5, 600, N'BIRTHDAY', 500, NULL, N'年度活動贈點', '2026-01-01 00:00:00', '2027-01-01 00:00:00', 200),
    -- REDEMPTION -300（order6 EXPIRED，FIFO從BIRTHDAY500扣，avail=200）
    (34, 5, 300, N'REDEMPTION', -300, N'6', N'兌換：記憶棉腰靠', '2026-03-12 08:30:00', '2027-03-12 08:30:00', NULL),
    -- BIRTHDAY +60（同年第二筆，屬活動回饋，保留原始邏輯）
    (35, 5, 360, N'BIRTHDAY', 60, NULL, N'會員活動回饋點數', '2026-03-10 11:00:00', '2027-03-10 11:00:00', 60),
    -- 即將到期點數（15天內）
    (36, 5, 410, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-05-31 00:00:00', '2026-05-31 00:00:00', 50),

    -- ══ cust 6 賈寶玉 ══
    -- WELCOME_BONUS +100
    (37, 6, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2025-07-01 00:00:00', '2026-07-01 00:00:00', 100),
    -- TRANSFER +591（transfer6，保持原始帳面值）
    (39, 6, 691, N'TRANSFER', 591, N'6', N'專車接送完成獲得點數', '2026-03-14 13:30:00', '2027-03-14 13:30:00', 591),
    -- 即將到期點數（15天內）
    (40, 6, 741, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-06-01 00:00:00', '2026-06-01 00:00:00', 50),

    -- ══ cust 7 許仙 ══
    -- WELCOME_BONUS +100
    (41, 7, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2025-06-01 00:00:00', '2026-06-01 00:00:00', 100),
    -- FIRST_RENTAL +50
    (43, 7, 190, N'FIRST_RENTAL', 50, N'1013', N'首次租車獎勵', '2026-03-03 10:10:00', '2027-03-03 10:10:00', 50),
    -- RENTAL +40（order1013，2000/50=40）
    (45, 7, 140, N'RENTAL', 40, N'1013', N'租車結案獲得點數', '2026-03-03 10:00:00', '2027-03-03 10:00:00', 40),
    -- 即將到期點數（15天內）
    (46, 7, 240, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-06-02 00:00:00', '2026-06-02 00:00:00', 50),

    -- ══ cust 8 白素貞 ══
    (47, 8, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2025-05-01 00:00:00', '2026-05-01 00:00:00', 0),
    (48, 8, 50, N'EXPIRED', -100, NULL, N'新會員贈點到期扣除', '2026-05-01 01:00:00', '2027-05-01 01:00:00', NULL),
       -- 修改：將建立時間改為 2025-05-27（即 2026-05-27 的一年前）
    (49, 8, 150, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-05-27 00:00:00', '2026-05-27 00:00:00', 50),

    -- ══ cust 9 小青 ══
    (50, 9, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2025-04-01 00:00:00', '2026-04-01 00:00:00', 0),
    (51, 9, 50, N'EXPIRED', -100, NULL, N'新會員贈點到期扣除', '2026-04-01 01:00:00', '2027-04-01 01:00:00', NULL),
    -- 修改：將建立時間改為 2025-05-28
    (52, 9, 150, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-05-28 00:00:00', '2026-05-28 00:00:00', 50),

    -- ══ cust 10 諸葛亮 ══
    (53, 10, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2025-03-01 00:00:00', '2026-03-01 00:00:00', 0),
    (54, 10, 50, N'EXPIRED', -100, NULL, N'新會員贈點到期扣除', '2026-03-01 01:00:00', '2027-03-01 01:00:00', NULL),
   -- 修改：將建立時間改為 2025-05-29
    (55, 10, 150, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-05-29 00:00:00', '2026-05-29 00:00:00', 50),

    -- ══ cust 11 劉備 ══
    (56, 11, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2025-02-01 00:00:00', '2026-02-01 00:00:00', 0),
    (57, 11, 50, N'EXPIRED', -100, NULL, N'新會員贈點到期扣除', '2026-02-01 01:00:00', '2027-02-01 01:00:00', NULL),
   -- 修改：將建立時間改為 2025-05-30
    (58, 11, 150, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-05-30 00:00:00', '2026-05-30 00:00:00', 50),


    -- ══ cust 12 關羽 ══
    (59, 12, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2025-01-01 00:00:00', '2026-01-01 00:00:00', 0),
    (60, 12, 50, N'EXPIRED', -100, NULL, N'新會員贈點到期扣除', '2026-01-01 01:00:00', '2027-01-01 01:00:00', NULL),
   -- 修改：將建立時間改為 2025-05-31
    (61, 12, 150, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-05-31 00:00:00', '2026-05-31 00:00:00', 50),


    -- ══ cust 13 張飛 ══
    (62, 13, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2024-12-01 00:00:00', '2025-12-01 00:00:00', 0),
    (63, 13, 50, N'EXPIRED', -100, NULL, N'新會員贈點到期扣除', '2025-12-01 01:00:00', '2026-12-01 01:00:00', NULL),
    -- 修改：將建立時間改為 2025-06-01
    (64, 13, 150, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-06-01 00:00:00', '2026-06-01 00:00:00', 50),


    -- ══ cust 14 貂蟬 ══
    (65, 14, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2024-11-01 00:00:00', '2025-11-01 00:00:00', 0),
    (66, 14, 50, N'EXPIRED', -100, NULL, N'新會員贈點到期扣除', '2025-11-01 01:00:00', '2026-11-01 01:00:00', NULL),
     -- 修改：將建立時間改為 2025-06-02
    (67, 14, 150, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-06-02 00:00:00', '2026-06-02 00:00:00', 50),


    -- ══ cust 15 呂布（停權）══
    (68, 15, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2024-10-01 00:00:00', '2025-10-01 00:00:00', 0),
    (69, 15, 50, N'EXPIRED', -100, NULL, N'新會員贈點到期扣除', '2025-10-01 01:00:00', '2026-10-01 01:00:00', NULL),
 -- 修改：將建立時間改為 2025-06-03
    (70, 15, 150, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-06-03 00:00:00', '2026-06-03 00:00:00', 50),

    -- ══ cust 16 曹操 ══
    (71, 16, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2024-09-01 00:00:00', '2025-09-01 00:00:00', 0),
    (72, 16, 50, N'EXPIRED', -100, NULL, N'新會員贈點到期扣除', '2025-09-01 01:00:00', '2026-09-01 01:00:00', NULL),
    -- 修改：將建立時間改為 2025-06-04
    (73, 16, 150, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-06-04 00:00:00', '2026-06-04 00:00:00', 50),


    -- ══ cust 17 孫權 ══
    (74, 17, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2024-08-01 00:00:00', '2025-08-01 00:00:00', 0),
    (75, 17, 50, N'EXPIRED', -100, NULL, N'新會員贈點到期扣除', '2025-08-01 01:00:00', '2026-08-01 01:00:00', NULL),
   -- 修改：將建立時間改為 2025-05-21
    (76, 17, 150, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-05-21 00:00:00', '2026-05-21 00:00:00', 50),


    -- ══ cust 18 周瑜 ══
    (77, 18, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2024-07-01 00:00:00', '2025-07-01 00:00:00', 0),
    (78, 18, 50, N'EXPIRED', -100, NULL, N'新會員贈點到期扣除', '2025-07-01 01:00:00', '2026-07-01 01:00:00', NULL),
   -- 修改：將建立時間改為 2025-05-22
    (79, 18, 150, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-05-22 00:00:00', '2026-05-22 00:00:00', 50),

    -- ══ cust 19 小喬 ══
    (80, 19, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2024-06-01 00:00:00', '2025-06-01 00:00:00', 0),
    (81, 19, 50, N'EXPIRED', -100, NULL, N'新會員贈點到期扣除', '2025-06-01 01:00:00', '2026-06-01 01:00:00', NULL),
      -- 修改：將建立時間改為 2025-05-23
    (82, 19, 150, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-05-23 00:00:00', '2026-05-23 00:00:00', 50),

    -- ══ cust 20 大喬 ══
    (83, 20, 100, N'WELCOME_BONUS', 100, NULL, N'新會員註冊贈點', '2024-05-01 00:00:00', '2025-05-01 00:00:00', 0),
    (84, 20, 50, N'EXPIRED', -100, NULL, N'新會員贈點到期扣除', '2025-05-01 01:00:00', '2026-05-01 01:00:00', NULL),
    -- 修改：將建立時間改為 2025-05-24
    (85, 20, 150, N'BIRTHDAY', 50, NULL, N'生日贈點', '2025-05-24 00:00:00', '2026-05-24 00:00:00', 50);
SET IDENTITY_INSERT points_history OFF;

-- ── points_rules ─────────────────────────────────────────────
SET IDENTITY_INSERT points_rules ON;
INSERT INTO points_rules
    (rule_id, rule_name, rule_key, ratio, description, is_active)
VALUES
    (1, N'租車換點', N'RENTAL_POINT', 0.02, N'每50元換1點，未滿50捨棄', 1),
    (2, N'專車換點', N'TRANSFER_POINT', 0.03, N'每33元換1點，未滿33捨棄', 1),
    (3, N'新會員首租獎勵', N'FIRST_RENTAL', 50.00, N'首次完成租車送50點', 1),
    (4, N'生日禮金', N'BIRTHDAY', 50.00, N'生日固定贈送50點，可由活動調整', 1),
    (5, N'點數過期扣除', N'EXPIRE', 1.00, N'年度點數過期歸零', 1),
    (6, N'新會員註冊贈點', N'WELCOME_BONUS', 100.00, N'註冊成功固定贈送100點', 1);
SET IDENTITY_INSERT points_rules OFF;


-- ── transfer_rate ────────────────────────────────────────────
SET IDENTITY_INSERT transfer_rate ON;
INSERT INTO transfer_rate
    (rate_id, rate_name, base_fee, per_km_fee, vehicle_type, is_active, created_at)
VALUES
    (1, N'經濟小型轎車方案', 100.00, 25.00, N'小型轎車', 1, '2026-01-01 00:00:00'),
    (2, N'標準中型轎車方案', 150.00, 30.00, N'中型轎車', 1, '2026-01-05 10:00:00'),
    (3, N'舒適休旅車方案', 220.00, 35.00, N'休旅車', 1, '2026-01-10 14:30:00'),
    (4, N'多人商旅廂型車方案', 500.00, 60.00, N'廂型車', 1, '2026-01-20 09:15:00'),
    (5, N'綠能電動車方案', 200.00, 35.00, N'電動車', 1, '2026-02-01 16:45:00');
SET IDENTITY_INSERT transfer_rate OFF;

-- ── transfer_order ───────────────────────────────────────────
SET IDENTITY_INSERT transfer_order ON;
INSERT INTO transfer_order
    (transfer_id, cust_id, cust_phone, driver_id, vehicle_id, rate_id, transfer_type, pickup_location, dropoff_location, scheduled_pickup_time, scheduled_dropoff_time, real_dropoff_time, passenger_count, start_mileage, end_mileage, luggage_count, total_amount, status, note, created_at)
VALUES
    (1, 5, N'0955-005-005', 1, 6, 3, N'接機', N'臺北市中正區台北車站東三門', N'桃園市大園區桃園國際機場第二航廈', '2026-05-10 08:30:00', '2026-05-10 10:00:00', NULL, 4, NULL, NULL, 3, 0.00, N'已取消', N'歷史取消單，移除接送中狀態', '2026-05-01 16:00:00'),
    (2, 1, N'0911-001-001', 3, 1, 2, N'送機', N'臺北市中正區台北總站服務中心', N'臺北市松山區松山機場國內線航廈', '2026-03-12 09:00:00', '2026-03-12 10:00:00', '2026-03-12 10:05:00', 2, 30500, 30620, 1, 3750.00, N'已完成', N'準時完成；費用依 Java 里程公式計算', '2026-03-01 11:00:00'),
    (3, 2, N'0922-002-002', 4, 7, 2, N'一般', N'新竹市東區新竹火車站前站', N'新竹縣竹北市新竹高鐵站4號出口', '2026-03-11 09:00:00', '2026-03-11 10:00:00', '2026-03-11 10:10:00', 3, 45280, 45300, 2, 750.00, N'已完成', N'車輛隔日才開始逾期租車；費用依 Java 里程公式計算', '2026-03-03 09:00:00'),
    (4, 3, N'0933-003-003', 5, 13, 1, N'接機', N'桃園市大園區桃園國際機場第一航廈', N'臺北市中正區台北總站服務中心', '2026-05-29 13:00:00', '2026-05-29 14:00:00', NULL, 2, NULL, NULL, 1, 0.00, N'已預訂', N'5/29 後未來預約，車輛目前可用', '2026-05-15 10:00:00'),
    (5, 4, N'0944-004-004', 2, NULL, 5, N'送機', N'新竹市東區新竹火車站前站', N'桃園市大園區桃園國際機場第二航廈', '2026-05-11 07:00:00', '2026-05-11 08:30:00', NULL, 1, NULL, NULL, 1, 0.00, N'已取消', N'客戶取消，未派車', '2026-05-01 08:00:00'),
    (6, 6, N'0966-006-006', 4, 4, 4, N'一般', N'臺北市信義區台北101大樓', N'臺北市南港區南港展覽館1館', '2026-02-20 11:00:00', '2026-02-20 12:00:00', '2026-02-20 12:05:00', 6, 66500, 66820, 4, 19700.00, N'已完成', N'退役前最後一次廂型車接送；費用依 Java 里程公式計算', '2026-02-10 13:00:00'),
    (7, 6, N'0966-006-006', 6, 14, 4, N'一般', N'新竹縣竹北市新竹高鐵站2號出口', N'新竹市東區新竹科學園區管理局', '2026-05-12 15:00:00', '2026-05-12 16:00:00', NULL, 5, NULL, NULL, 3, 0.00, N'已取消', N'歷史取消單，移除處理中狀態', '2026-05-01 14:30:00'),
    (8, 7, N'0977-007-007', 7, 21, 1, N'一般', N'新竹市東區新竹火車站前站', N'新竹市北區新竹市政府', '2026-05-30 09:00:00', '2026-05-30 10:00:00', NULL, 2, NULL, NULL, 1, 0.00, N'已預訂', N'5/29 後小型車接送預約', '2026-05-15 09:00:00'),
    (9, 8, N'0978-008-008', 8, 22, 3, N'接機', N'桃園市大園區桃園國際機場第一航廈', N'新竹市東區新竹火車站前站', '2026-06-01 10:00:00', '2026-06-01 11:30:00', NULL, 4, NULL, NULL, 2, 0.00, N'已預訂', N'5/29 後休旅車接機預約', '2026-05-15 09:10:00'),
    (10, 9, N'0979-009-009', 9, NULL, 2, N'送機', N'臺北市松山區饒河街觀光夜市', N'臺北市松山區松山機場國內線航廈', '2026-05-13 07:00:00', '2026-05-13 08:00:00', NULL, 1, NULL, NULL, 1, 0.00, N'已取消', N'客戶取消，未派車', '2026-05-01 09:20:00'),
    (11, 10, N'0980-010-010', 7, 28, 2, N'一般', N'臺北市中正區台北總站服務中心', N'臺北市信義區台北101大樓', '2026-06-03 13:00:00', '2026-06-03 14:00:00', NULL, 2, NULL, NULL, 0, 0.00, N'已預訂', N'5/29 後中型轎車市區接送', '2026-05-15 09:30:00'),
    (12, 11, N'0981-011-011', 8, 29, 4, N'一般', N'新竹市東區新竹火車站前站', N'新竹縣竹北市新竹高鐵站4號出口', '2026-06-04 09:00:00', '2026-06-04 10:00:00', NULL, 6, NULL, NULL, 4, 0.00, N'已預訂', N'5/29 後廂型車多人接送', '2026-05-15 09:40:00'),
    (13, 12, N'0982-012-012', 9, 13, 1, N'一般', N'臺北市中正區台北總站服務中心', N'臺北市中正區台北車站東三門', '2026-06-05 09:00:00', '2026-06-05 10:00:00', NULL, 1, NULL, NULL, 1, 0.00, N'已預訂', N'5/29 後小型車短程接送', '2026-05-15 09:50:00'),
    (14, 13, N'0983-013-013', 8, 22, 3, N'接機', N'桃園市大園區桃園國際機場第二航廈', N'新竹市東區新竹科學園區管理局', '2026-06-06 14:00:00', '2026-06-06 15:30:00', NULL, 3, NULL, NULL, 2, 0.00, N'已預訂', N'5/29 後休旅車機場接送', '2026-05-15 10:00:00'),
    (15, 14, N'0984-014-014', 7, 21, 1, N'送機', N'新竹市東區新竹火車站前站', N'桃園市大園區桃園國際機場第一航廈', '2026-06-10 06:00:00', '2026-06-10 07:30:00', NULL, 2, NULL, NULL, 2, 0.00, N'已預訂', N'5/29 後小型車送機預約', '2026-05-15 10:10:00'),
    (16, 15, N'0985-015-015', 8, NULL, 5, N'一般', N'新竹市北區新竹城隍廟', N'新竹縣竹北市新竹高鐵站4號出口', '2026-05-14 12:00:00', '2026-05-14 13:00:00', NULL, 1, NULL, NULL, 1, 0.00, N'已取消', N'停權會員測試取消單', '2026-05-01 10:20:00'),
    (17, 16, N'0986-016-016', 9, 23, 5, N'一般', N'臺北市中正區台北總站服務中心', N'臺北市南港區南港軟體園區二期', '2026-06-12 09:00:00', '2026-06-12 10:00:00', NULL, 2, NULL, NULL, 1, 0.00, N'已預訂', N'5/29 後電動車接送預約', '2026-05-15 10:30:00'),
    (18, 17, N'0987-017-017', 10, 24, 5, N'一般', N'新竹市東區新竹火車站前站', N'新竹縣竹北市高鐵新竹站特定區', '2026-06-12 15:00:00', '2026-06-12 16:00:00', NULL, 2, NULL, NULL, 1, 0.00, N'已預訂', N'5/29 後未來接送預約', '2026-05-15 10:40:00'),
    (19, 18, N'0988-018-018', 7, 28, 2, N'送機', N'臺北市信義區市政府捷運站2號出口', N'桃園市大園區桃園國際機場第二航廈', '2026-06-15 05:30:00', '2026-06-15 07:00:00', NULL, 2, NULL, NULL, 2, 0.00, N'已預訂', N'5/29 後中型轎車送機預約', '2026-05-15 10:50:00'),
    (20, 19, N'0989-019-019', 8, 29, 4, N'一般', N'新竹市東區新竹火車站前站', N'新竹縣竹北市竹北市公所', '2026-06-18 11:00:00', '2026-06-18 12:00:00', NULL, 5, NULL, NULL, 3, 0.00, N'已預訂', N'5/29 後廂型車市區接送預約', '2026-05-15 11:00:00');
SET IDENTITY_INSERT transfer_order OFF;

-- ── used_car ─────────────────────────────────────────────────
SET IDENTITY_INSERT used_car ON;
INSERT INTO used_car
    (usedcar_id, vehicle_id, asking_price, condition_desc, list_date, expire_date, status, created_time)
VALUES
    (1, 4, 350000.00, N'GRANVIA 退役車，完整保養紀錄', '2026-03-01', '2026-06-01', N'SOLD', '2026-03-01 09:00:00'),
    (2, 8, 520000.00, N'Sienta 退役車，內裝良好', '2026-02-20', '2026-05-15', N'SOLD', '2026-02-20 10:00:00'),
    (3, 9, 300000.00, N'Yaris 退役車，適合市區代步', '2026-02-15', '2026-05-15', N'SOLD', '2026-02-15 11:00:00'),
    (4, 10, 620000.00, N'GRANVIA 商務車，里程合理', '2026-02-25', '2026-05-15', N'SOLD', '2026-02-25 08:00:00'),
    (5, 11, 800000.00, N'Model 3 電池健康度良好，上架中樣本', '2026-03-05', '2026-06-05', N'ACTIVE', '2026-03-05 09:00:00'),
    (6, 12, 430000.00, N'CR-V 休旅車，下架樣本', '2026-02-28', '2026-05-15', N'REMOVED', '2026-02-28 14:00:00'),
    (7, 17, 260000.00, N'Altis 退役車，上架待售', '2026-03-10', '2026-06-10', N'ACTIVE', '2026-03-10 09:00:00'),
    (8, 18, 390000.00, N'Sienta 退役車，內裝磨耗明顯，暫時下架', '2026-02-10', '2026-05-10', N'REMOVED', '2026-02-10 09:00:00'),
    (9, 19, 180000.00, N'Yaris 高里程退役車，低價上架', '2026-03-12', '2026-06-12', N'ACTIVE', '2026-03-12 09:00:00'),
    (10, 20, 560000.00, N'GRANVIA 退役商務車，已成交', '2026-03-01', '2026-06-01', N'SOLD', '2026-03-01 09:00:00');
SET IDENTITY_INSERT used_car OFF;

-- ── viewing_appointment ──────────────────────────────────────
SET IDENTITY_INSERT viewing_appointment ON;
INSERT INTO viewing_appointment
    (appt_id, usedcar_id, cust_id, appt_time, status, location_id, message, notes)
VALUES
    (1, 5, 2, '2026-03-18 10:00:00', N'PENDING', 1, N'想看Model 3車況', N'待客服確認'),
    (2, 5, 5, '2026-03-20 14:00:00', N'CONFIRMED', 2, N'對Model 3有興趣', N'已聯繫客戶並保留時段'),
    (3, 3, 1, '2026-03-07 11:00:00', N'COMPLETED', 1, N'確認Yaris車況', N'成交前看車完成'),
    (4, 4, 3, '2026-03-08 09:00:00', N'COMPLETED', 2, N'GRANVIA商務用途詢問', N'看車完成'),
    (5, 2, 6, '2026-03-02 15:00:00', N'COMPLETED', 2, N'Sienta試看', N'成交前看車完成'),
    (6, 6, 4, '2026-03-10 10:00:00', N'CANCELLED', 1, N'取消看車', N'客戶有事取消');
SET IDENTITY_INSERT viewing_appointment OFF;

-- ── sales_record ─────────────────────────────────────────────
SET IDENTITY_INSERT sales_record ON;
INSERT INTO sales_record
    (sale_id, usedcar_id, cust_id, buyer_name, buyer_phone, buyer_idno, final_price, payment_method, pay_status, sale_date, emp_id, notes)
VALUES
    (1, 1, 4, N'唐三藏', N'0944-004-004', N'C456789012', 342000.00, N'CREDIT_CARD', N'PAID', '2026-03-14', 2, N'綠界回寫完成付款'),
    (2, 2, 2, N'趙敏', N'0922-002-002', N'A234567890', 505000.00, N'TRANSFER', N'PENDING', '2026-03-06', 2, N'已建立銷售單，待匯款確認'),
    (3, 3, 1, N'李小龍', N'0911-001-001', N'Z123456789', 295000.00, N'CASH', N'CANCELLED', '2026-03-10', 1, N'付款取消但保留銷售紀錄供測試'),
    (4, 4, 3, N'孫悟空', N'0933-003-003', N'B345678901', 610000.00, N'TRANSFER', N'PAID', '2026-03-12', 2, N'used_car 4 狀態對齊 SOLD'),
    (5, 10, 20, N'大喬', N'0990-020-020', N'S823456789', 555000.00, N'CREDIT_CARD', N'PAID', '2026-03-15', 17, N'新增 used_car 10 成交紀錄');
SET IDENTITY_INSERT sales_record OFF;

-- ── dispatch_log ─────────────────────────────────────────────
-- PENDING,待執行 / FINISHED,已完成；每筆調度固定預留 2 小時 CLEANING 清潔窗
SET IDENTITY_INSERT dispatch_log ON;
INSERT INTO dispatch_log
    (dispatch_id, vehicle_id, from_location_id, to_location_id, driver_id, scheduled_start_time, actual_start_time, actual_end_time, start_mileage, end_mileage, emp_id, reason, status, notes, created_at, updated_at)
VALUES
    (1, 1, 2, 1, 1, '2026-03-04 07:00:00', '2026-03-04 07:00:00', '2026-03-04 09:00:00', 29880, 30000, 2, N'台北短租需求', N'FINISHED', N'清潔窗 07:00-09:00，完成後車輛在台北，接續 1001', '2026-03-03 17:00:00', '2026-03-04 09:00:00'),
    (2, 7, 1, 2, 2, '2026-03-07 06:00:00', '2026-03-07 06:00:00', '2026-03-07 08:00:00', 44880, 45000, 2, N'新竹取車需求', N'FINISHED', N'清潔窗 06:00-08:00，完成後車輛在新竹，接續 1002', '2026-03-06 16:00:00', '2026-03-07 08:00:00'),
    (3, 3, 2, 1, 3, '2026-03-13 08:00:00', '2026-03-13 08:00:00', '2026-03-13 10:00:00', 53100, 53200, 2, N'台北取車需求', N'FINISHED', N'清潔窗 08:00-10:00，完成後車輛在台北', '2026-03-12 12:00:00', '2026-03-13 10:00:00'),
    (4, 1, 1, 2, 3, '2026-05-29 07:00:00', NULL, NULL, NULL, NULL, 2, N'新竹展店備車', N'PENDING', N'5/29 後待執行調度；尚未開始，實際時間與里程保留 NULL', '2026-05-15 13:00:00', '2026-05-15 13:00:00'),
    (5, 5, 1, 2, 2, '2026-05-30 13:00:00', NULL, NULL, NULL, NULL, 1, N'新竹站電動車需求', N'PENDING', N'5/29 後待執行調度；尚未開始，實際時間與里程保留 NULL', '2026-05-15 09:00:00', '2026-05-15 13:05:00'),
    (6, 6, 1, 2, 4, '2026-06-01 15:00:00', NULL, NULL, NULL, NULL, 2, N'未來接送後回新竹', N'PENDING', N'5/29 後待執行調度；尚未開始，實際時間與里程保留 NULL', '2026-05-15 16:00:00', '2026-05-15 10:30:00'),
    (7, 21, 1, 2, 7, '2026-03-04 08:00:00', '2026-03-04 08:00:00', '2026-03-04 10:00:00', 15000, 15200, 14, N'新竹站補車', N'FINISHED', N'清潔窗 08:00-10:00，完成後車輛停放新竹站', '2026-03-03 16:00:00', '2026-03-04 10:00:00'),
    (8, 22, 2, 1, 8, '2026-06-02 08:00:00', NULL, NULL, NULL, NULL, 14, N'台北總站活動備車', N'PENDING', N'5/29 後待執行調度；尚未開始，實際時間與里程保留 NULL', '2026-05-15 12:00:00', '2026-05-15 12:00:00'),
    (9, 23, 1, 2, 9, '2026-06-03 08:00:00', NULL, NULL, NULL, NULL, 15, N'新竹支援車', N'PENDING', N'5/29 後待執行調度；尚未開始，實際時間與里程保留 NULL', '2026-05-15 12:10:00', '2026-05-15 12:20:00'),
    (10, 24, 1, 2, 10, '2026-06-04 11:00:00', NULL, NULL, NULL, NULL, 17, N'新竹站電動車臨時支援', N'PENDING', N'5/29 後待執行調度；尚未開始，實際時間與里程保留 NULL', '2026-05-15 10:30:00', '2026-05-15 11:05:00');
SET IDENTITY_INSERT dispatch_log OFF;

-- ── cross_location_fee ───────────────────────────────────────
SET IDENTITY_INSERT cross_location_fee ON;
INSERT INTO cross_location_fee
    (fee_id, from_location_id, to_location_id, extra_fee)
VALUES
    (1, 1, 2, 500.00),
    (2, 2, 1, 500.00);
SET IDENTITY_INSERT cross_location_fee OFF;

GO

PRINT N'=== 租車管理系統資料庫建立完成：Mock Data 已依 2026-03-15 對齊 ===';

