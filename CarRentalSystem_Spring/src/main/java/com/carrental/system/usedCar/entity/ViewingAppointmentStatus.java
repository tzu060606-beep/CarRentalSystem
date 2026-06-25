package com.carrental.system.usedCar.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ViewingAppointmentStatus {

	PENDING("PENDING","待確認"),  //（剛收到資料）
	CONFIRMED("CONFIRMED","已確認預約"),  //(聯繫過客戶）
	COMPLETED("COMPLETED","已完成看車"),
	CANCELLED("CANCELLED","已取消");
	
	private final String dbCode; //存入資料庫的代碼
	private final String description; //顯示要用的中文
	
	private ViewingAppointmentStatus(String dbCode, String description) {
		this.dbCode = dbCode;
		this.description = description;
	}
	
	//讓converter可用dbCode反查回enum的靜態方法
		public static ViewingAppointmentStatus fromDbCode(String dbCode) {
			if (dbCode == null) {
				return null;
			}
			for (ViewingAppointmentStatus status : ViewingAppointmentStatus.values()) {
				if (status.getDbCode().equals(dbCode)) {
					return status;
				}
			}
			throw new IllegalArgumentException("未知的車輛狀態代碼：" + dbCode);
		}
}
