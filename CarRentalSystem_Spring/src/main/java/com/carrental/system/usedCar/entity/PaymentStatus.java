package com.carrental.system.usedCar.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum PaymentStatus {

	PENDING("PENDING", "待付款"),
	PAID("PAID", "已付款"),
	CANCELLED("CANCELLED", "已取消");

	private final String dbCode;
	private final String description;

	private PaymentStatus(String dbCode, String description) {
		this.dbCode = dbCode;
		this.description = description;
	}

	public static PaymentStatus fromDbCode(String dbCode) {
		if (dbCode == null) {
			return null;
		}
		for (PaymentStatus status : PaymentStatus.values()) {
			if (status.getDbCode().equals(dbCode)) {
				return status;
			}
		}
		throw new IllegalArgumentException("未知的狀態代碼" + dbCode);
	}

}
