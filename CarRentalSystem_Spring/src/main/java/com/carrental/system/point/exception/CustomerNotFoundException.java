package com.carrental.system.point.exception;
//找不到客戶
public class CustomerNotFoundException extends ResourceNotFoundException {

	public CustomerNotFoundException(String message) {
		super(message);
	}

}
