package com.carrental.system.usedCar.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import com.carrental.system.login.entity.CustomerBean;
import com.carrental.system.login.entity.EmployeeBean;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Entity @Table(name = "sales_record")
@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
@SoftDelete(strategy = SoftDeleteType.DELETED, columnName = "is_deleted")
public class SalesRecordBean {
	
	@Id @Column(name = "sale_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer saleId; // 成交編號
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "usedcar_id")
	private UsedCarBean usedCar; // 待售編號
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "cust_id")
	private CustomerBean customer; // 客戶編號
	
	@NonNull
	private String buyerName; // 買受人姓名
	
	@NonNull
	private String buyerPhone; // 買家聯絡電話
	
	@NonNull
	private String buyerIdno; // 買家身分證字號
	
	private BigDecimal finalPrice; // 成交價格
	
	@NonNull
	@Enumerated(EnumType.STRING)
	private PaymentMethod paymentMethod; // 付款方式 (1:現金,2:信用卡,3:轉帳)
//	CASH("現金"),CREDIT_CARD("信用卡"),TRANSFER("轉帳"),
	
	@NonNull
	@Enumerated(EnumType.STRING)
	private PaymentStatus payStatus; // 付款狀態
	//PENDING: 待付款, PAID: 已付款, CANCELLED: 已取消
	
//	@NonNull
	@JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate saleDate; // 成交日期
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "emp_id")
	private EmployeeBean emp; // 經手員工編號
	
	private String notes; // 成交備註

	public SalesRecordBean(UsedCarBean usedCar, CustomerBean customer, @NonNull String buyerName, @NonNull String buyerPhone,
			@NonNull String buyerIdno, BigDecimal finalPrice, @NonNull PaymentMethod paymentMethod,
			@NonNull LocalDate saleDate, EmployeeBean emp, String notes) {
		super();
		this.usedCar = usedCar;
		this.customer = customer;
		this.buyerName = buyerName;
		this.buyerPhone = buyerPhone;
		this.buyerIdno = buyerIdno;
		this.finalPrice = finalPrice;
		this.paymentMethod = paymentMethod;
		this.saleDate = saleDate;
		this.emp = emp;
		this.notes = notes;
	}
}
