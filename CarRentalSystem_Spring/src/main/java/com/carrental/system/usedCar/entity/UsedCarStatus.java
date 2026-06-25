package com.carrental.system.usedCar.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum UsedCarStatus {
	
	ACTIVE("ACTIVE","上架"),
	SOLD("SOLD","已售"),
	REMOVED("REMOVED","下架");

	private final String dbCode;
	private final String description;
	
	UsedCarStatus(String dbCode,String description){
		this.dbCode = dbCode;
		this.description = description;
	}
	
	public static UsedCarStatus fromDbCode(String dbCode) {
		if(dbCode == null) {
			return null;
		}
		for(UsedCarStatus status : UsedCarStatus.values()) {
			if(status.getDbCode().equals(dbCode)) {
				return status;
			}
		}
		throw new IllegalArgumentException("未知的狀態代碼" + dbCode);
	}
	
}
