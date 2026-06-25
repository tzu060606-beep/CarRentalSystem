package com.carrental.system.usedCar.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.springframework.stereotype.Component;

import com.carrental.system.vehicle.entity.Vehicle;

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

@Entity @Table(name = "used_car")
@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
@SoftDelete(strategy = SoftDeleteType.DELETED, columnName = "is_deleted")
public class UsedCarBean {
	
	@Id @Column(name = "usedcar_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer usedCarId; // 二手車編號
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "vehicle_id")
	private Vehicle vehicle; // 原本車輛編號
	
	@NonNull
	private BigDecimal askingPrice; // 開價/預售價格
	
	@NonNull
	private String conditionDesc; // 車況描述
	
	@NonNull
	private LocalDate listDate; // 上架日期
	
	@NonNull
	private LocalDate expireDate; // 刊登截止日
	
	@NonNull
	@Enumerated(EnumType.STRING) // 告訴 JPA 資料庫存字串 "ACTIVE
	private UsedCarStatus status; // 出售狀態 (上架/已售/下架)
//	ACTIVE,上架 /SOLD,已售/REMOVED,下架
	
	@CreationTimestamp
	@Column(name = "created_time", updatable = false)
	private LocalDateTime createdTime; // 資料建立時間
	

	public UsedCarBean(Vehicle vehicle, @NonNull BigDecimal askingPrice, @NonNull String conditionDesc,
			@NonNull LocalDate listDate, @NonNull LocalDate expireDate, @NonNull UsedCarStatus status,
			@NonNull LocalDateTime createdTime) {
		super();
		this.vehicle = vehicle;
		this.askingPrice = askingPrice;
		this.conditionDesc = conditionDesc;
		this.listDate = listDate;
		this.expireDate = expireDate;
		this.status = status;
		this.createdTime = createdTime;
	}

	
	
}


