package com.carrental.system.login.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class EmployeeRequestDTO {
    private String empName;
    private String empAccount;
    private String empPassword;
    private String empRole;
    private String empPhone;
    private String empEmail;
    private String empStatus;
    private LocalDate resignDate;  // 離職日期
    private String empPhoto;
}
