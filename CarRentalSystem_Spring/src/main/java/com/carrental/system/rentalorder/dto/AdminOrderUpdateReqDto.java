package com.carrental.system.rentalorder.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.carrental.system.rentalorder.entity.RentalPlansBean;
import com.carrental.system.rentalorder.enums.OrderStatus;
import com.carrental.system.rentalorder.enums.PayStatus;
import com.carrental.system.rentalorder.enums.PaymentMethod;

import lombok.Data;

@Data
public class AdminOrderUpdateReqDto {

    // 1. 識別欄位
    private Integer orderId;

    // 2. 觸發「重算租金」的關鍵欄位
    private Integer planId;       
    private LocalDateTime pickupTime; 
    private LocalDateTime returnTime;  

    // 3. 據點與車輛調度 (重要)
    private Integer pickupLocationId;
    private Integer returnLocationId;
    private Integer vehicleId;       // 車輛調度用，若變更需連動更新車輛狀態

    // 4. 付款與訂單狀態 
    private OrderStatus orderStatus; 
    private PayStatus payStatus;     
    private PaymentMethod depositPayMethod; 
    private PaymentMethod balancePayMethod; 

    // 5. 備註、發票與合約資訊 (依你的資料表型態定義)
    private String orderRemark;
    private String invoiceNo;       // 如果是字串請改為 String invoiceNumber
    private String contract;         // 如果是關聯 ID 請改為 Integer contractId

    // 6. 手動金額覆蓋 
    private BigDecimal rentalFee;    
    private BigDecimal extraFee;

    // 7. 子表詳細資訊
    private AdminShortTermUpdateDto shortTerm;
    private AdminLongTermUpdateDto longTerm;
    
}
