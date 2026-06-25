package com.carrental.system.point.exception;
//庫存不足
public class InsufficientStockException extends BusinessRuleException {

	public InsufficientStockException(String message) {
		super(message);
	}

}
