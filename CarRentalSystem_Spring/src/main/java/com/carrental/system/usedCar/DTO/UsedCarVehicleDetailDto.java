package com.carrental.system.usedCar.DTO;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Date;

import com.carrental.system.usedCar.entity.UsedCarStatus;
import com.carrental.system.vehicle.entity.LocationStatus;
import com.carrental.system.vehicle.entity.VehicleStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsedCarVehicleDetailDto {

	// --- 來自 UsedCarBean 的欄位 (你剛貼的部分) ---
    private Integer usedCarId;      // 二手車編號
    private BigDecimal askingPrice; // 價格
    private String conditionDesc;   // 車況描述
    private LocalDate listDate;     // 上架日期
    private UsedCarStatus usedCarStatus; // 出售狀態
	
	private Integer vehicleId;
	private String plateNo;
	private VehicleStatus status;
	private String color;
	@JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
	private Date manufactureDate;
	@JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
	private Date issuedDate;
	@JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
	private Date inspectionDate;
	private Integer mileage;
	private Integer nextMaintainenceMileage;
	private String description;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	private Timestamp createdTime;

//	vehicle
	private Integer modelId;
	private String brand;
	private String modelName;
	private Integer displacement;
	private Double turningRadius;
	private String vehicleType;
	private Integer seats;
	private Integer luggageCapacity;
	private String fuelType;
	private String transmission;
	private Boolean wheelchairAvailable;
	private String vehicleImgUrl;

//	location
	private Integer locationId;
	private String locationName;
	private String address;
	private String phone;
	private Integer parkingCapacity;
	private LocationStatus locationStatus;
}
