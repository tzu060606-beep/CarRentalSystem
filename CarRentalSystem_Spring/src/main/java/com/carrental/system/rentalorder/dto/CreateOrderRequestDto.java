package com.carrental.system.rentalorder.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;


import lombok.Data;

@Data
public class CreateOrderRequestDto {

     private Long orderId;         // 訂單編號
     private String orderStatus;   // 狀態 (RESERVED, PICKED_UP...)
      
     //直接幫前端把名字轉出來，Vue 直接印就好
     private String customerName;  // 客戶姓名 (從 Entity 的 customer 拿)
     private String plateNo;       // 車牌號碼 (從 Entity 的 vehicle 拿)
     private String planName;      // 方案名稱 (從 Entity 的 plan 拿)
      
     // 時間與金額 (通通給前端顯示)
     private LocalDateTime pickupTime;
     private LocalDateTime returnTime;
     private BigDecimal rentalFee;
     private BigDecimal extraFee;
     private BigDecimal totalAmount;
      
     // 🌟 明細欄位直接扁平化塞進來 (把 Detail 拆開來放)
     private LocalDateTime actualPickupTime;
     private LocalDateTime actualReturnTime;
     private Integer startMileage;
     private Integer endMileage;
    
    
}
