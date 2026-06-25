package com.carrental.system.rentalorder.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PayStatus {
	
	UNPAID("未付款"),
    DEPOSIT_PAID("已付訂金"),
    PAID("支付完成"),
    REFUNDING("退款中"),
    REFUNDED("已退款");

    private final String description;

    PayStatus(String description) {
        this.description = description;
    }


    @JsonValue
    public String getDescription() { 
    	return description; 
    	
    }

    @JsonCreator
    public static PayStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        for (PayStatus status : PayStatus.values()) {
            if (status.name().equalsIgnoreCase(value) || status.description.equals(value)) {
                return status;
            }
        }
        return null; // 或者丟出例外
    }

}
