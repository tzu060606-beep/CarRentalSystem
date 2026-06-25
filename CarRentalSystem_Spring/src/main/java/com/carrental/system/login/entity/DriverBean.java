package com.carrental.system.login.entity;

import java.time.LocalDateTime;

import com.carrental.system.vehicle.entity.Vehicle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "driver")
public class DriverBean {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer driverId;//driver_id INT IDENTITY(1,1) NOT NULL,   
    @Column(name = "emp_id")
    private Integer empId; //emp_id INT,
    private String licenseNo;//license_no NVARCHAR(20),
    private LocalDateTime licenseExpiry; //license_expiry DATETIME2,
    private Boolean petAvailable; //pet_available BIT DEFAULT 0,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_vehicle_id")
    private Vehicle vehicle; //fixed_vehicle_id INT,FK//-- 【新增】用來固定綁定車輛

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "emp_id", insertable = false, updatable = false)
    private EmployeeBean employeeBean;
}
