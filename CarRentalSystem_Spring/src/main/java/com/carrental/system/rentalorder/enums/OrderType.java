package com.carrental.system.rentalorder.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderType {
	
	SHORT_TERM("日租"),
    LONG_TERM("長租");

    private final String description;

    OrderType(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() { 
    	return description; 
    	
    }

    @JsonCreator
    public static OrderType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        for (OrderType type : OrderType.values()) {
            if (type.name().equalsIgnoreCase(value) || type.description.equals(value)) {
                return type;
            }
        }
        return null;
    }

}
