package com.carrental.system.login.dto;

import java.time.LocalDateTime;
import com.carrental.system.vehicle.entity.Vehicle;
import lombok.Data;

@Data
public class DriverRequestDTO {
    private Integer empId;
    private String licenseNo;
    private LocalDateTime licenseExpiry;
    private Boolean petAvailable;
    private Vehicle vehicle;
}
