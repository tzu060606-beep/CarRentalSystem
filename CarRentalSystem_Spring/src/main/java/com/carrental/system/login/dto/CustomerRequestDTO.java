package com.carrental.system.login.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class CustomerRequestDTO {
    private String custName;
    private String custPhone;
    private String custEmail;
    private String custAccount;
    private String custPassword;
    private String custAddress;
    private String custLicense;
    private LocalDate custLicenseExpiry;
    private String custStatus;
    private Integer currentPoints;
    private Integer totalAccumulated;
    private String avatarBase64;
    private String custAvatar;
}
