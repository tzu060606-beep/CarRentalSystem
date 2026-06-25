package com.carrental.system.usedCar.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum PaymentMethod {

	CASH("CASH", "現金"), 
	CREDIT_CARD("CREDIT_CARD", "信用卡"),
	TRANSFER("TRANSFER", "轉帳");

	private final String dbCode;
	private final String description;

	private PaymentMethod(String dbCode, String description) {
		this.dbCode = dbCode;
		this.description = description;
	}

	public static PaymentMethod fromDbCode(String dbCode) {
		if (dbCode == null) {
			return null;
		}
		for (PaymentMethod status : PaymentMethod.values()) {
			if (status.getDbCode().equals(dbCode)) {
				return status;
			}
		}
		throw new IllegalArgumentException("未知的狀態代碼" + dbCode);
	}
}
