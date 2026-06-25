package com.carrental.system.login.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class CustomerResponseDTO {
    private Integer custId;
    private String custName;
    private String custPhone;
    private String custEmail;
    private String custAccount;
    private String custAddress;
    private String custLicense;
    private LocalDate custLicenseExpiry;
    private String custStatus;
    private Integer currentPoints;
    private Integer totalAccumulated;
    private String custAvatar;
}
