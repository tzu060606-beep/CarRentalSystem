package com.carrental.system.login.entity;



import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.carrental.system.vehicle.entity.DispatchLog;
import com.fasterxml.jackson.annotation.JsonIgnore;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employee")
public class EmployeeBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer empId;          // → emp_id

    private String empName;         // → emp_name
    private String empAccount;      // → emp_account
    private String empPassword;     // → emp_password
    private String empRole;         // → emp_role
    private String empPhone;        // → emp_phone
    private String empEmail;        // → emp_email
    private String empStatus;       // → emp_status (在職/離職)
    private LocalDate resignDate;   // → resign_date (離職日期，在職時為 null)
    private String empPhoto;        // → emp_photo
    private LocalDateTime createdAt;  // → created_at
    private LocalDateTime updatedAt;  // → updated_at
    
	// 串「dispatch_log」by文
	@OneToMany(mappedBy = "employeeBean", cascade = CascadeType.ALL)
	@JsonIgnore
	public List<DispatchLog> dispatchLogs;
}
