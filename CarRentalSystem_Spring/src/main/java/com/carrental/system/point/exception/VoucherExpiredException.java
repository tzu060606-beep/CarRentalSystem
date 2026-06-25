package com.carrental.system.point.exception;
//voucher已過期
public class VoucherExpiredException extends BusinessRuleException {

	public VoucherExpiredException(String message) {
		super(message);
	}

}
