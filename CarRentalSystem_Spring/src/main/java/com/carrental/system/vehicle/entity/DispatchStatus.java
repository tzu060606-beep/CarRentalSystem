package com.carrental.system.vehicle.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import tools.jackson.databind.jsonFormatVisitors.JsonValueFormat;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum DispatchStatus {
	
	//PENDING,待執行/IN_PROCESS,調度中/FINISHED,已完成/CANCEL,已取消
	PENDING("PENDING","待執行"),
	IN_PROCESS("IN_PROCESS","調度中"),
	FINISHED("FINISHED","已完成"),
	CANCEL("CANCEL","已取消");
	
	private final String dbCode;
	private final String description;
	
	private DispatchStatus(String dbCode, String description) {
		this.dbCode = dbCode;
		this.description = description;
	}
	
	//讓converter可用dbCode反查回enum的靜態方法
	public static DispatchStatus fromDbCode(String dbCode) {
		if (dbCode == null) return null;
		for (DispatchStatus status : DispatchStatus.values()) {
			if (status.getDbCode().equals(dbCode)) return status;
		}
		throw new IllegalArgumentException("未知的調度狀態代碼：" + dbCode);
	}
	
	//狀態轉換邏輯
	public boolean canTransitionTo(DispatchStatus nextStatus) {
		return switch (this) {
		case PENDING ->
			nextStatus == IN_PROCESS || nextStatus == CANCEL;
		case IN_PROCESS ->
			nextStatus == FINISHED || nextStatus == CANCEL;
		case FINISHED ->
			false;
		case CANCEL ->
			false;
		default ->
			false;
		};	
	}
}
