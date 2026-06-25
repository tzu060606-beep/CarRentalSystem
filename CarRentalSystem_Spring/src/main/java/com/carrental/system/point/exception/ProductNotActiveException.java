package com.carrental.system.point.exception;
//商品下架中
public class ProductNotActiveException extends BusinessRuleException {

	public ProductNotActiveException(String message) {
		super(message);
	}

}
