package com.carrental.system.usedCar.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.carrental.system.usedCar.entity.UsedCarStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsedCarDto {
	
	// 只准讀取（回傳前端），不准寫入（禁止前端透過 JSON 傳進來修改）
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Integer usedCarId; // 二手車編號
	
	private Integer vehicleId; // 原本車輛編號
	
	@NonNull
	@Min(value = 0, message = "開價不能小於 0")
	private BigDecimal askingPrice; // 開價/預售價格
	
	@NonNull
	private String conditionDesc; // 車況描述
	
	@NonNull
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate listDate; // 上架日期
	
	@NonNull
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate expireDate; // 刊登截止日
	
	@NonNull
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	private UsedCarStatus status; // 出售狀態 (上架/已售/下架)
//	ACTIVE,上架 /SOLD,已售/REMOVED,下架
	
	// 只准讀取（回傳前端），不准寫入（禁止前端透過 JSON 傳進來修改）
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @CreationTimestamp
	private LocalDateTime createdTime; // 資料建立時間
}
