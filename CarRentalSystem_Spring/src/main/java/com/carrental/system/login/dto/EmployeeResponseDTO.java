package com.carrental.system.login.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class EmployeeResponseDTO {
    private Integer empId;
    private String empName;
    private String empAccount;
    private String empRole;
    private String empPhone;
    private String empEmail;
    private String empStatus;
    private LocalDate resignDate;  // 離職日期
    private String empPhoto;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
