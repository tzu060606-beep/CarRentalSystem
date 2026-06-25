package com.carrental.system.rentalorder.dto;



import java.time.LocalDateTime;

import com.carrental.system.rentalorder.enums.FuelLevel;

import lombok.Data;

@Data
public class AdminShortTermUpdateDto {

    private Integer detailId;
    private Integer orderId;             // INT (FK)
    private LocalDateTime actualPickupTime;
    private LocalDateTime actualReturnTime;
    private Integer startMileage;        // INT
    private Integer endMileage;          // INT
    private String noteDesc;         // NVARCHAR
    private FuelLevel fuelLevelReturn;  // NVARCHAR
    
}
