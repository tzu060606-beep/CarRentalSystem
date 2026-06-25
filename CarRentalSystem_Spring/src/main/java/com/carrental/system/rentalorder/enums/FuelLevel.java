package com.carrental.system.rentalorder.enums;

public enum FuelLevel {
	
	FULL("滿油", 0),
    HIGH("四分之三", 0),
    HALF("一半", 300),
    LOW("四分之一", 600),
    EMPTY("見底", 900);

    private final String description;
    private final int penaltyFee;

    FuelLevel(String description, int penaltyFee) {
        this.description = description;
        this.penaltyFee = penaltyFee;
    }

    public String getDescription() { return description; }
    public int getPenaltyFee() { return penaltyFee; }

}
