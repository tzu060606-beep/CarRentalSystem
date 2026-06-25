package com.carrental.system.vehicle.entity;

import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.stereotype.Component;

import com.carrental.system.rentalorder.entity.RentalPlansBean;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity @Table(name = "car_model")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
// @SQLDelete：將deleteByID()實作的內容覆寫為修改，並寫入sql指令，當程式呼叫 delete() 時，Hibernate 不會下 DELETE，而是改下這句 UPDATE
@SQLDelete(sql = "UPDATE car_model SET is_deleted = 1 WHERE model_id = ?")
// @SQLRestriction：全局查詢濾鏡。所有透過 Hibernate 發出的 SELECT，都會自動在後面補上 AND is_deleted = 0(啟用狀態)
@SQLRestriction("is_deleted = 0")
public class CarModel {

	@Id @Column(name = "model_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer modelId; //model_id INT IDENTITY(1,1) NOT NULL,-- 車款ID
	private String brand; //brand NVARCHAR(50) NOT NULL,-- 品牌
	private String modelName; //model_name NVARCHAR(50) NOT NULL,-- 型號
	private Integer displacement; //displacement INT,-- 排氣量
	private Double turningRadius; //turning_radius DECIMAL(3,1),-- 迴轉半徑
	private String vehicleType; //vehicle_type NVARCHAR(30) NOT NULL,-- 車型：小型轎車/中型轎車/休旅車/廂型車/電動車
	private Integer seats; //seats INT NOT NULL,-- 座位數
	private Integer luggageCapacity; //luggage_capacity INT,-- 行李數
	private String fuelType; //fuel_type NVARCHAR(10),-- 動力：汽油/柴油/純電/油電混合
	private String transmission; //transmission NVARCHAR(5),-- 變速箱：自排/手排
	private Boolean wheelchairAvailable; //wheelchair_available BIT DEFAULT 0,-- 輪椅友善
	private String vehicleImgUrl; //vehicle_img_url NVARCHAR(255),-- 圖片
	
	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false; // --軟刪除 (0:正常, 1:已刪除),預設為false(0)啟用
	
//	一對多
//	告訴 Hibernate：「這是一個關聯表，請幫我把旗下的車輛抓出來。」
	@OneToMany(mappedBy = "carModel", cascade = CascadeType.ALL)
//	@Transient //告訴 Hibernate：「請完全無視這個變數，當作它不存在，不要去資料庫做任何事。」
	@JsonIgnore
	public List<Vehicle> vehicles;

	public CarModel(@NonNull String brand, @NonNull String modelName, Integer displacement, Double turningRadius,
			@NonNull String vehicleType, @NonNull Integer seats, Integer luggageCapacity, String fuelType,
			String transmission, Boolean wheelchairAvailable, String vehicleImgUrl, Boolean isDeleted) {
		super();
		this.brand = brand;
		this.modelName = modelName;
		this.displacement = displacement;
		this.turningRadius = turningRadius;
		this.vehicleType = vehicleType;
		this.seats = seats;
		this.luggageCapacity = luggageCapacity;
		this.fuelType = fuelType;
		this.transmission = transmission;
		this.wheelchairAvailable = wheelchairAvailable;
		this.vehicleImgUrl = vehicleImgUrl;
		this.isDeleted = isDeleted;
	}



	
	
}
