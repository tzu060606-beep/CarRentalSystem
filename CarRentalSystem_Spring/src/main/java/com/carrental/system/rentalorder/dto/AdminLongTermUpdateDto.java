package com.carrental.system.rentalorder.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AdminLongTermUpdateDto {

    private int orderId;             // INT (FK)

    private LocalDateTime actualPickupTime;
    private LocalDateTime actualReturnTime;

    private Integer contractMonths;      // INT 
    private BigDecimal monthlyPayment; // DECIMAL
    private Integer billingDay;          // INT
    private Integer paidMonths;          // INT 已經付了幾個月

    private String deliveryAddress;  // NVARCHAR

    private Integer startMileage;        // INT
    private Integer endMileage;          // INT

    private String noteDesc;         // NVARCHAR
    
}
