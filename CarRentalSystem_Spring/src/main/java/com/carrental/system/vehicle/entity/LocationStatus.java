package com.carrental.system.vehicle.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatTypes;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum LocationStatus {

	//OPERATING,營運中/REST,暫停營業/CANCEL,撤銷據點
	OPERATING("OPERATING","營運中"),
	REST("REST","暫停營業"),
	CANCEL("CANCEL","撤銷據點");
	
	private final String dbCode;
	private final String description;
	
	//constrctor
	private LocationStatus(String dbCode, String description) {
		this.dbCode = dbCode;
		this.description = description;
	}
	
	public static LocationStatus fromDbCode(String dbCode) {
		if (dbCode == null) {
			return null;
		}
		for (LocationStatus status : LocationStatus.values()) {
			if (status.getDbCode().equals(dbCode)) {
				return status;
			}
		}
		throw new IllegalArgumentException("未知的據點狀態代碼：" + dbCode);
	}
	
	public boolean canTransitionTo(LocationStatus nexStatus) {
		return switch (this) {
		case OPERATING ->
			nexStatus == REST || nexStatus == CANCEL;
		case REST ->
			nexStatus == OPERATING || nexStatus == CANCEL;
		default ->
			false;
		};
	}
}
