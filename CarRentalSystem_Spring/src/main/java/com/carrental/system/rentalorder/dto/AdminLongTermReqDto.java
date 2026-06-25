package com.carrental.system.rentalorder.dto;

import java.math.BigDecimal;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AdminLongTermReqDto {

    private int orderId;             // INT (FK)
    private LocalDateTime actualPickupTime;
    private LocalDateTime actualReturnTime;
    private Integer contractMonths;      // INT
    private BigDecimal monthlyPayment; // DECIMAL
    private Integer billingDay;          // INT
    private Integer paidMonths;          // INT
    private String deliveryAddress;  // NVARCHAR
    private Integer startMileage;        // INT
    private Integer endMileage;          // INT
    private String noteDesc;         // NVARCHAR
    
}
