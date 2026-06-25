package com.carrental.system.point.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.LastModifiedDate;

import com.carrental.system.login.entity.CustomerBean;
import com.carrental.system.point.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "redemption_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
//@EntityListeners(AuditingEntityListener.class)
public class RedemptionOrder {

	@Id
	@Column(name = "redemption_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer redemptionId;

	@ManyToOne
	@JoinColumn(name = "cust_id")
	private CustomerBean customerBean;

	// TODO:訂單表頁面需要顯示商品名稱跟客戶名稱而非ID

	@ManyToOne
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(name = "product_quantity")
	private Integer productQuantity;

	@Column(name = "points_spent")
	private Integer pointsSpent;

	@Enumerated(EnumType.STRING) // 讓jpa存英文字串而不是數字
	@Column(name = "order_status")
	private OrderStatus orderStatus;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@Column(name = "create_time", updatable = false)
	private LocalDateTime createTime;

	// TODO:欄位新增
//	@LastModifiedDate//自動更新最後修改的時間點
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@Column(name = "update_time")
	private LocalDateTime updateTime;

}
