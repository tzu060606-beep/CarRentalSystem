package com.carrental.system.usedCar.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.springframework.stereotype.Component;

import com.carrental.system.login.entity.CustomerBean;
import com.carrental.system.vehicle.entity.Location;
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

@Entity
@Table(name = "viewing_appointment")
@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
@SoftDelete(strategy = SoftDeleteType.DELETED, columnName = "is_deleted")
public class ViewingAppointmentBean {

	@Id
	@Column(name = "appt_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer apptId; // 賞車預約編號

	// 1. 改成物件關聯 (usedcar_id)
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usedcar_id",nullable = false)
    private UsedCarBean usedCar;  // 待售車輛編號

	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cust_id")
    private CustomerBean customer;  // 客戶編號

	@NonNull
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm[:ss]")
	private LocalDateTime apptTime; // 預約時間

	@NonNull
	@Enumerated(EnumType.STRING)
	private ViewingAppointmentStatus status; // 預計改預約狀態 (待確認/已預訂/已完成看車/已取消)
//	PENDING,待確認（剛收到資料）/CONFIRMED,已預定(聯繫過客戶）/
//	COMPLETED，已完成看車/CANCELLED,已取消
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "location_id") // 對應資料庫的欄位// 位置編號
	private Location location; 

	private String message; // 預約訊息

	private String notes; // 預約備註

	public ViewingAppointmentBean(UsedCarBean usedCar, CustomerBean customer, @NonNull LocalDateTime apptTime, @NonNull ViewingAppointmentStatus status,
			Location location, String message, String notes) {
		super();
		this.usedCar = usedCar;
		this.customer = customer;
		this.apptTime = apptTime;
		this.status = status;
		this.location = location;
		this.message = message;
		this.notes = notes;
	}


}
