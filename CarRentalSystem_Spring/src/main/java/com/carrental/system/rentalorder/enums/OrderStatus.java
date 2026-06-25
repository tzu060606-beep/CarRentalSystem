package com.carrental.system.rentalorder.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {
	
	RESERVED("已預約"),
    PICKED_UP("已取車"),
    RETURNED("已歸還(待檢查)"),
    OVERDUE("逾期未還"),
    CLOSED("已結案"),
    CANCELLED("已取消");

    private final String description;


    
    OrderStatus(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() { 
    	
    	return description; 
    	
    }

    @JsonCreator
    public static OrderStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        for (OrderStatus status : OrderStatus.values()) {
            if (status.name().equalsIgnoreCase(value) || status.description.equals(value)) {
                return status;
            }
        }
        return null;
    }

}
