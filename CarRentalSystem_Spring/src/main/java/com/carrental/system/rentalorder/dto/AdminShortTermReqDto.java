package com.carrental.system.rentalorder.dto;



import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AdminShortTermReqDto {


    
    private int orderId;             // INT (FK)
    private LocalDateTime actualPickupTime;
    private LocalDateTime actualReturnTime;
    private Integer startMileage;        // INT
    private Integer endMileage;          // INT
    private String noteDesc;         // NVARCHAR
    private String fuelLevelReturn;  // NVARCHAR
    
}
