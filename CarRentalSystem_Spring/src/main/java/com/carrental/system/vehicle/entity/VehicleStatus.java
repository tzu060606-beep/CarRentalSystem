package com.carrental.system.vehicle.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum VehicleStatus {

	// TODO: 要新增「已被預訂(租車)」和「已被預訂(專車接送)」？
	//場內(整理中)/場內(可出租/派車)/出租中/場內(待維修)/維修中/場內(待調度)/調度中/專車接送中/退役待處置
	CLEANING("CLEANING", "場內(整理中)"), //可出租、待維修、待調度  ，對應 DispathcLog的 FINISHED
	AVAILABLE("AVAILABLE", "場內(可接單)"), //出租中、整理中  ，對應 DispathcLog的 CANCEL
	RENTING("RENTING", "出租中"), //整理中
	// TOBE_MAINTAINED("TOBE_MAINTAINED", "場內(待維修)"), //維修中
	MAINTAINING("MAINTAINING", "維修中"), //整理中
	// TOBE_DISPATCHED("TOBE_DISPATCHED", "場內(待調度)"), //調度中、整理中 ，對應 DispathcLog的 PENDING
	DISPATCHING("DISPATCHING", "調度中"), //整理中 ，對應 DispathcLog的 IN_PROCESS
	SHUTTLING("SHUTTLING", "專車接送中"), //整理中
	RETIRED("RETIRED", "退役待處置");
	
	private final String dbCode; //存入資料庫的代碼
	private final String description; //顯示要用的中文
	
	//constructor
	VehicleStatus(String dbCode,String description){
		this.dbCode = dbCode;
		this.description = description;
	}
	
	//讓converter可用dbCode反查回enum的靜態方法
	public static VehicleStatus fromDbCode(String dbCode) {
		if (dbCode == null) {
			return null;
		}
		for (VehicleStatus status : VehicleStatus.values()) {
			if (status.getDbCode().equals(dbCode)) {
				return status;
			}
		}
		throw new IllegalArgumentException("未知的車輛狀態代碼：" + dbCode);
	}
	
	//狀態轉換邏輯
	public boolean canTransitionTo(VehicleStatus nextStatus) {
		// 轉換為retired的條件 & 前一個狀態
		if (nextStatus == RETIRED) {
			return this != RENTING && this != DISPATCHING && this != SHUTTLING;
		}
		return switch (this) {

			case CLEANING ->
				nextStatus == AVAILABLE || nextStatus == MAINTAINING || nextStatus == RENTING || nextStatus == SHUTTLING || nextStatus == DISPATCHING;
			case AVAILABLE ->
				nextStatus == CLEANING || nextStatus == MAINTAINING || nextStatus == RENTING || nextStatus == SHUTTLING || nextStatus == DISPATCHING || nextStatus == AVAILABLE;
			case RENTING ->
				nextStatus == CLEANING || nextStatus == MAINTAINING ;
			case DISPATCHING ->
				nextStatus == CLEANING || nextStatus == MAINTAINING || nextStatus == AVAILABLE;
			case SHUTTLING ->
				nextStatus == CLEANING || nextStatus == MAINTAINING ;
			case MAINTAINING ->
				nextStatus == CLEANING;
			case RETIRED ->
//				nextStatus == AVAILABLE || nextStatus == MAINTAINING ;
				false;
			default ->
				false;
		};
	}
}
