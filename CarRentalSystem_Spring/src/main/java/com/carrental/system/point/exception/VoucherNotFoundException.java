package com.carrental.system.point.exception;
//找不到兌換券
public class VoucherNotFoundException extends ResourceNotFoundException{

	public VoucherNotFoundException(String message) {
		super(message);
	}

}
