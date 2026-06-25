package com.carrental.system.usedCar.DTO;

import java.time.LocalDateTime;

import com.carrental.system.usedCar.entity.ViewingAppointmentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ViewingAppointmentDto {
	
	// 只准讀取（回傳前端），不准寫入（禁止前端透過 JSON 傳進來修改）
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Integer apptId; // 賞車預約編號

	private Integer usedCarId; // 待售車輛編號

	private Integer custId; // 客戶編號

	@NonNull
	@JsonFormat(pattern = "yyyy-MM-dd[' ']['T']HH:mm[:ss]")
	private LocalDateTime apptTime; // 預約時間

	@NonNull
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	private ViewingAppointmentStatus status; // 預計改預約狀態 (待確認/已預訂/已完成看車/已取消)
//	PENDING,待確認（剛收到資料）/CONFIRMED,已預定(聯繫過客戶）/
//	COMPLETED，已完成看車/CANCELLED,已取消

	private Integer locationId; // 位置編號
	
	private String locationName;

	private String message; // 預約訊息

	private String notes; // 預約備註
	
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Integer queueCount;

}
