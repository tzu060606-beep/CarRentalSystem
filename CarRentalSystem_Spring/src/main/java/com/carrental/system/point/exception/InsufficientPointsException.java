package com.carrental.system.point.exception;
//點數不足
public class InsufficientPointsException extends BusinessRuleException {

	public InsufficientPointsException(String message) {
		super(message);
	}

}
