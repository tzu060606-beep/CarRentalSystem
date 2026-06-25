# 🚗 OneRent 租車管理系統

> 全端租車管理系統，提供客戶線上租車、員工後台管理等完整功能。  
> 此為資策會課程小組專題，本人主要負責 **登入/註冊系統** 與 **員工及客戶資料管理** 模組。

---

## 📌 專案簡介

OneRent 是一套完整的租車管理系統，涵蓋前台客戶租車流程與後台管理功能。系統採前後端分離架構，前端使用 Vue 3，後端使用 Spring Boot，資料庫使用 MS SQL Server。

---

## 🛠️ 技術架構

### 後端（Backend）
| 技術 | 說明 |
|------|------|
| Spring Boot 4.0 | 後端框架 |
| Spring Security | 身分驗證與授權 |
| JWT (JJWT) | Token-based 身分驗證 |
| Spring Data JPA | ORM 資料存取 |
| OAuth 2.0 | Google 第三方登入 |
| Spring Mail | 忘記密碼信件發送 |
| MapStruct | DTO 物件映射 |
| Lombok | 簡化程式碼 |

### 前端（Frontend）
| 技術 | 說明 |
|------|------|
| Vue 3 | 前端框架 |
| Vite | 開發建置工具 |
| Vue Router | 路由管理 |
| Pinia | 狀態管理 |
| Axios | HTTP 請求 |
| Bootstrap 5 | UI 元件庫 |
| Element Plus | 進階 UI 元件 |
| Google Maps API | 地圖功能整合 |

### 資料庫
| 技術 | 說明 |
|------|------|
| MS SQL Server | 關聯式資料庫 |

### 第三方服務
| 服務 | 說明 |
|------|------|
| Google OAuth 2.0 | 第三方登入 |
| Google Calendar API | 行事曆整合 |
| 綠界 ECPay | 金流 / 電子發票 / 物流 |

---

## 📁 專案結構

```
CarRentalSystem/
├── CarRentalSystem_Spring/     # 後端 - Spring Boot
│   ├── src/main/java/com/carrental/system/
│   │   ├── login/              # 🔑 登入模組（本人負責）
│   │   ├── vehicle/            # 車輛管理
│   │   ├── rentalorder/        # 租車訂單
│   │   ├── point/              # 點數系統
│   │   ├── usedCar/            # 二手車 / Google Calendar
│   │   └── transfer/           # 轉帳功能
│   └── src/main/resources/
│
├── CarRentalSystem_Vue/        # 前端 - Vue 3
│   └── src/
│       ├── views/
│       │   ├── admin/login/    # 🔑 後台管理頁面（本人負責）
│       │   ├── customer/login/ # 🔑 客戶登入頁面（本人負責）
│       │   └── ...
│       ├── router/             # 路由設定
│       └── store/              # 狀態管理
│
└── README.md
```

---

## 🔑 我負責的模組

### 1. 登入 / 註冊系統

- **JWT 身分驗證**：實作 Token-based 登入機制，包含 Token 產生、驗證與 Filter 攔截
- **Spring Security 整合**：設定安全策略、API 路徑權限控管
- **Google OAuth 2.0 登入**：第三方社群登入整合
- **忘記密碼**：透過 Email 發送重設密碼連結（含 Token 過期驗證）
- **角色權限控制**：區分員工（Admin）與客戶（Customer）角色

### 2. 員工管理（後台）

- 員工資料 CRUD
- 員工帳號建立與編輯
- 員工列表查詢與搜尋

### 3. 客戶資料管理

- 客戶註冊與資料驗證
- 客戶個人資料編輯
- 客戶列表查詢與搜尋
- 駕駛人資料管理

### 相關檔案

<details>
<summary>📂 後端 - 點擊展開</summary>

**Controller**
- `AuthController.java` — 登入/註冊/忘記密碼 API
- `CustomerController.java` — 客戶資料 CRUD API
- `EmployeeController.java` — 員工資料 CRUD API
- `DriverController.java` — 駕駛人資料 API

**Security**
- `SecurityConfig.java` — Spring Security 設定
- `JwtUtil.java` — JWT Token 產生與解析
- `JwtAuthenticationFilter.java` — JWT 驗證 Filter

**Service / Repository / Entity / DTO**
- 完整的分層架構（Controller → Service → Repository → Entity）

</details>

<details>
<summary>📂 前端 - 點擊展開</summary>

**後台管理**
- `EmployeeLoginView.vue` — 員工登入頁
- `EmployeeView.vue` — 員工管理列表
- `EmployeeEditView.vue` — 員工資料編輯
- `CustomerListView.vue` — 客戶列表
- `CustomerAddView.vue` — 新增客戶
- `CustomerEditView.vue` — 編輯客戶
- `CustomerSearchView.vue` — 搜尋客戶

**客戶前台**
- `CustomerLoginView.vue` — 客戶登入頁
- `CustomerRegisterView.vue` — 客戶註冊頁
- `CustomerProfileView.vue` — 個人資料頁
- `ForgotPasswordView.vue` — 忘記密碼頁
- `ResetPasswordView.vue` — 重設密碼頁

</details>

---

## ⚙️ 環境建置

### 前置需求

- Java 17+
- Node.js 20+
- MS SQL Server
- Maven 3.9+

### 後端啟動

```bash
cd CarRentalSystem_Spring

# 1. 複製設定檔範本並填入你的資料庫資訊
cp src/main/resources/application.properties.example src/main/resources/application.properties

# 2. 啟動 Spring Boot
./mvnw spring-boot:run
```

### 前端啟動

```bash
cd CarRentalSystem_Vue

# 1. 複製環境變數範本
cp .env.example .env

# 2. 安裝依賴
npm install

# 3. 啟動開發伺服器
npm run dev
```

---

## 👥 團隊成員

此專案為 **資策會課程小組專題**，由團隊成員分工完成。

---

## 📄 License

此專案僅供學習用途。
