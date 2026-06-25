package com.carrental.system.rentalorder.entity;

import java.math.BigDecimal;

import java.sql.Timestamp;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.stereotype.Component;

import com.carrental.system.login.entity.CustomerBean;
import com.carrental.system.rentalorder.enums.OrderStatus;
import com.carrental.system.rentalorder.enums.OrderType;
import com.carrental.system.rentalorder.enums.PayStatus;
import com.carrental.system.rentalorder.enums.PaymentMethod;
import com.carrental.system.vehicle.entity.Location;
import com.carrental.system.vehicle.entity.Vehicle;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Entity
@Table(name = "rental_order")
@Getter // 替代 @Data，安全拿值
@Setter // 替代 @Data，安全設值
@Builder // 拯救 19 個欄位的救星
@NoArgsConstructor // JPA 規定 Entity 必須要有空建構子！
@AllArgsConstructor // 這是給 @Builder 底層運作使用的，我們自己不要去呼叫它
@SQLDelete(sql = "UPDATE rental_order SET is_deleted = 1 WHERE order_id = ?") // 攔截 deleteById，改成 Update
@SQLRestriction("is_deleted = 0")//只要呼叫任何查詢，例如 findAll()、findById()、甚至連 Join 查詢）Hibernate 都會自動補上
//所以如果要查刪除的，要用原生SQL繞過
//@Query(value = "SELECT * FROM rental_order", nativeQuery = true)

@EntityListeners(AuditingEntityListener.class) // 🌟 感應器：監控資料變動
public class RentalOrderBean {
	
	

	@Id // 標記為主鍵
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 設定為自動遞增 (Identity)
    @Column(name = "order_id") // 指定資料庫欄位名稱	
		private Integer orderId;             // INT //order_id
	
	// @Column(name = "cust_id")
	//     private Integer custId;              // INT //cust_id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cust_id")
	private CustomerBean customer;//給師姊這個變數
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicle_id")
	private Vehicle vehicle;//給文姊這個變數
	
	@Enumerated(EnumType.STRING)
	@Column(name = "order_type")
    private OrderType orderType;       // NVARCHAR //order_type
	    
	// @Column(name = "plan_id")
	//     private Integer planId;              // INT //plan_id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "plan_id")
	private RentalPlansBean rentalPlan;
	    
	// INT //pickup_location_id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pickup_location_id")
	private Location pickupLocation;//給文姊這個變數
	    
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "return_location_id")
	private Location returnLocation;//給文姊這個變數
	
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")//這裡必須加上時間格式，才能讓程式知道沒有T的能直接進行轉換
	@Column(name = "pickup_time")
	    private LocalDateTime pickupTime; // DATETIME2 //pickup_time
	
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@Column(name = "return_time") 
	    private LocalDateTime returnTime; // DATETIME2 //return_time
	    
	@Column(name = "rental_fee")
	    private BigDecimal rentalFee; // DECIMAL (為了精準度，錢不用 double) //rental_fee
	    
	@Column(name = "extra_fee")
	    private BigDecimal extraFee;  // DECIMAL //extra_fee
	    
	    private BigDecimal deposit;             // INT //deposit
	    
	@Column(name = "total_amount")
	    private BigDecimal totalAmount; // DECIMAL //total_amount
	
	@Enumerated(EnumType.STRING)    
	@Column(name = "pay_status")    
    private PayStatus payStatus;       // NVARCHAR //pay_status
	
	@CreatedDate //新增時自動抓時間填入
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")   
	@Column(name = "order_time")    
	    private LocalDateTime orderTime;    // DATETIME2 //order_time
	
	@Enumerated(EnumType.STRING)
	@Column(name = "order_status")    
    private OrderStatus orderStatus;    // NVARCHAR //order_status
	
	@Enumerated(EnumType.STRING)
	@Column(name = "deposit_pay_method")   
    private PaymentMethod depositPayMethod; // 訂金付款方式
	
	@Enumerated(EnumType.STRING)
	@Column(name = "balance_pay_method")   
    private PaymentMethod balancePayMethod; // 尾款付款方式

	@Column(name = "order_remark")//備註
	private String orderRemark;
	    
	@Column(name = "invoice_no")   
	    private String invoiceNo;           // String //invoice_no
	    
		private String contract;         // NVARCHAR(255) //contract

	@LastModifiedDate // 修改時自動更新時間
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
	
	@Column(name = "is_deleted")
	    private boolean deleted; 
		
	// 計算尾款專用，不存入資料庫
	@Transient
	    private java.math.BigDecimal remainingBalance; // 剩餘應付金額 (尾款)

	// 新增：一對一短租明細（一張訂單最多一張短租明細）
	@OneToOne(mappedBy = "rentalOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private ShortTermDetailBean shortTermDetail;
	// 新增：一對一長租明細（一張訂單最多一張長租明細）
	@OneToOne(mappedBy = "rentalOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private LongTermDetailBean longTermDetail;
	  
		
		/*先註解，以後有CustomerBean時使用
		@ManyToOne
		@JoinColumn(name = "cust_id") // 告訴 JPA：去資料庫找 cust_id 這個外鍵欄位來對應
		private CustomerBean customer; 
		*/
		
	
	    
	//   ---底下為JOIN其他表的欄位---
	    /*
		@Transient
	    private String custName;
	    @Transient
	    private String vehicleBrand;
	    @Transient
	    private String vehicleType;
	    @Transient
	    private String planName;
	    @Transient
	    private String pickupLocationName;
	    @Transient
	    private String returnLocationName;
		public int getOrderId() {
			return orderId;
		}*/


		
		/*
		 * 之後對接會員表時使用
		public CustomerBean getCustomer() {
			return customer;
		}
		public void setCustomer(CustomerBean customer) {
			this.customer = customer;
		}
	    */
	  
	    
	    

}
