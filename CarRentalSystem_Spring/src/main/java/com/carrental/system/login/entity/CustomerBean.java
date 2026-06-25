package com.carrental.system.login.entity;



import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customer")
public class CustomerBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer custId;              // → cust_id

    private String custName;             // → cust_name
    private String custPhone;            // → cust_phone
    private String custEmail;            // → cust_email
    private String custAccount;          // → cust_account
    private String custPassword;         // → cust_password
    private String custAddress;          // → cust_address
    private String custLicense;          // → cust_license
    private LocalDate custLicenseExpiry; // → cust_license_expiry
    private String custStatus;           // → cust_status
    private Integer currentPoints;       // → current_points
    private Integer totalAccumulated;    // → total_accumulated
    private String custAvatar;           // → cust_avatar

    // 忘記密碼功能
    private String resetToken;               // → reset_token
    private LocalDateTime resetTokenExpiry;   // → reset_token_expiry
}
