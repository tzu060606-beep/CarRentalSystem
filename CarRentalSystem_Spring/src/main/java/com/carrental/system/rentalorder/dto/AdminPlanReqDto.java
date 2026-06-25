package com.carrental.system.rentalorder.dto;

import java.math.BigDecimal;



import lombok.Data;

@Data
public class AdminPlanReqDto {
      
      private String planName;
      private String planDesc; // 描述可以允許空白
      private Boolean isLongTerm; // 建議用物件型別 Boolean 而不是基本型別 boolean，方便接 null 報錯
      private String appliedVehicleType;
      private BigDecimal basePrice;
      private BigDecimal overtimeFee; // 如果是長租可能不需要逾時費，所以不一定要加 @NotNull
      private Integer mileageLimit; // null 代表無限里程
      private BigDecimal excessMileageFee;
      private Boolean active;
    
}
