package com.carrental.system.vehicle.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.carrental.system.login.entity.DriverBean;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

// TODO: @Data不推，因為內含override hashcode()，建議拿掉@data，並另補toString(exclude="")
@Entity @Table(name = "vehicle")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@ToString //(exclude = )
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE vehicle SET is_deleted = 1 WHERE vehicle_id = ?")
@SQLRestriction("is_deleted = 0")
public class Vehicle {
	
	@Id @Column(name = "vehicle_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer vehicleId; //vehicle_id INT IDENTITY(1,1) NOT NULL, -- 車輛ID，
	private String plateNo;  //plate_no NVARCHAR(20) NOT NULL, UQ-- 車牌號碼
	
	// TODO: 加上@Enumerated(EnumType.STRING)
	private VehicleStatus status; //status NVARCHAR(30) DEFAULT '場內(整理中)' NOT NULL, -- 狀態：場內(整理中)/場內(可出租/派車)/出租中/維修中/調度中/專車接送中/退役待處置
	
	private String color; //color NVARCHAR(20), -- 車色：搭配色碼表做成select選單
	private LocalDate manufactureDate;  //manufacture_date DATE, -- 出廠日期
	private LocalDate issuedDate; //issued_date DATE NOT NULL, -- 發照日期(車齡計算的基準)
	private LocalDate inspectionDate; //inspection_date DATE, -- 法定驗車日(發照日期＋五年，邏輯寫在service)
	private Integer mileage; //mileage INT NOT NULL, -- 目前里程數
	private Integer nextMaintainenceMileage; //next_maintainence_mileage INT, -- 下次需維修里程數(通常每10000公里一次，邏輯寫在service)
	private String description; //description NVARCHAR(MAX), -- 車況詳述
	@CreatedDate
	@Column(name = "created_time", updatable = false)
	private LocalDateTime createdTime; //created_time DATETIME2 DEFAULT GETDATE(), -- 資料建立時間
	
	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false; // --軟刪除 (0:正常, 1:已刪除),預設為false(0)啟用
	
	
	// 人一我多
	// 串「location」
	// TODO:(fetch = FetchType.LAZY)避免，不需每次呼叫時取用對方所有欄位，
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_id", referencedColumnName = "location_id")
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	private Location location;
	
	// 串「car_model」
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "model_id", referencedColumnName = "model_id")
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	private CarModel carModel;
	
	// 我0..1人1 (driver表將vehicle_id設為FK)Driver是關係的「維護端 (Owner)」，Vehicle 是「被維護端 (Inverse side)」
	@OneToOne(mappedBy = "vehicle", fetch = FetchType.LAZY)
	@JsonIgnore
	private DriverBean driverBean;
	
	// 我一人多
	// 串「dispatch_log」
	@OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
	@JsonIgnore
	public List<DispatchLog> dispatchLogs;
	
	// TODO: 確定JsonIgnore必要性
	// TODO: mapped後續按對方entity內定義的變數名修改
//	// 串「rental_order」
//	@OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
//	@JsonIgnore
//	public List<RentalOrderBean> rentalOrderBeans;
	
//	// 串「transfer_order」
//	@OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
//	@JsonIgnore
//	public List<TransferOrder> transferOrders;
	
//	// 串「dispatch_log」
//	@OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
//	@JsonIgnore
//	public List<UsedCarBean> usedCarBeans;
	
}