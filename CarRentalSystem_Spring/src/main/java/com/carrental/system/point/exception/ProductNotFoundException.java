package com.carrental.system.point.exception;
//找不到商品
public class ProductNotFoundException extends ResourceNotFoundException {

	public ProductNotFoundException(String message) {
		super(message);
	}

}
