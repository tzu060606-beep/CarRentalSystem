package com.carrental.system.usedCar.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.carrental.system.usedCar.entity.PaymentMethod;
import com.carrental.system.usedCar.entity.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatTypes;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesRecordDto {

	// 只准讀取（回傳前端），不准寫入（禁止前端透過 JSON 傳進來修改）
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Integer saleId; // 成交編號

	private Integer usedCarId; // 待售編號

	private Integer custId; // 客戶編號

	@NonNull
	private String buyerName; // 買受人姓名

	@NonNull
	private String buyerPhone; // 買家聯絡電話

	@NonNull
	private String buyerIdno; // 買家身分證字號
	
	@Min(value = 0, message = "開價不能小於 0")
	private BigDecimal finalPrice; // 成交價格
	
	@NonNull
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	private PaymentMethod paymentMethod; // 付款方式 (1:現金,2:信用卡,3:轉帳)
	//CASH("現金"),CREDIT_CARD("信用卡"),TRANSFER("轉帳"),
	
	@NonNull
	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	private PaymentStatus payStatus; // 付款狀態
	//PENDING: 待付款, PAID: 已付款, CANCELLED: 已取消

//	@NonNull
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate saleDate; // 成交日期

	private Integer empId; // 經手員工編號

	private String notes; // 成交備註
}
