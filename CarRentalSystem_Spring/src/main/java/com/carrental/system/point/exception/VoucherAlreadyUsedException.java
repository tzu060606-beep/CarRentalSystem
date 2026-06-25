package com.carrental.system.point.exception;
//voucher已使用過
public class VoucherAlreadyUsedException extends BusinessRuleException {

	public VoucherAlreadyUsedException(String message) {
		super(message);
	}

}
