package com.carrental.system.rentalorder.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.carrental.system.rentalorder.enums.OrderStatus;
import com.carrental.system.rentalorder.enums.OrderType;
import com.carrental.system.rentalorder.enums.PayStatus;
import com.carrental.system.rentalorder.enums.PaymentMethod;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class AdminOrderReqDto {

    private Integer custId;              // INT //cust_id
    private Integer vehicleId;           // INT //vehicle_id
    private OrderType orderType;       // NVARCHAR //order_type
    private Integer planId;              // INT //plan_id
    private Integer pickupLocationId;    // INT //pickup_location_id
    private Integer returnLocationId;    // INT //return_location_id
    private LocalDateTime pickupTime; // DATETIME2 //pickup_time
    private LocalDateTime returnTime; // DATETIME2 //return_time
    private java.math.BigDecimal rentalFee; // DECIMAL (為了精準度，錢不用 double) //rental_fee
    private java.math.BigDecimal extraFee;  // DECIMAL //extra_fee
    private BigDecimal deposit;             //deposit
    private java.math.BigDecimal totalAmount; // DECIMAL //total_amount
    private PayStatus payStatus;    // NVARCHAR //pay_status
    private LocalDateTime orderTime;    // DATETIME2 //order_time
    private OrderStatus orderStatus;     // NVARCHAR //order_status
    private PaymentMethod depositPayMethod; 
    private PaymentMethod balancePayMethod;
    private String orderRemark;//備註
    private String invoiceNo;           // INT //invoice_No
    private String contract;         // NVARCHAR(255) //contract
    private java.math.BigDecimal remainingBalance;

    private AdminShortTermReqDto shortTerm;
    private AdminLongTermReqDto longTerm;

    
}
