package com.carrental.system.vehicle.entity;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.stereotype.Component;

import com.carrental.system.rentalorder.entity.RentalOrderBean;
import com.carrental.system.usedCar.entity.ViewingAppointmentBean;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity @Table(name = "location")
//@Data
@Getter
@Setter
@ToString //(exclude = )
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
//@SQLDelete：將deleteByID()實作的內容覆寫為修改，並寫入sql指令，當程式呼叫 delete() 時，Hibernate 不會下 DELETE，而是改下這句 UPDATE
@SQLDelete(sql = "UPDATE location SET is_deleted = 1 WHERE location_id = ?")
//@SQLRestriction：全局查詢濾鏡。所有透過 Hibernate 發出的 SELECT，都會自動在後面補上 AND is_deleted = 0(啟用狀態)
@SQLRestriction("is_deleted = 0")
public class Location {

	@Id @Column(name = "location_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer locationId; //location_id INT IDENTITY(1,1) NOT NULL,
	@NonNull
	private String locationName;  //location_name NVARCHAR(50) NOT NULL,
	private String address;  //address NVARCHAR(255),
	private String phone;  //phone NVARCHAR(20),
	private Integer parkingCapacity;  //parking_capacity INT,
	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false; // --軟刪除 (0:正常, 1:已刪除),預設為false(0)啟用
	
	// TODO: 加上@Enumerated(EnumType.STRING)
	@Enumerated(EnumType.STRING)
	private LocationStatus locationStatus;  //location_status NVARCHAR(10),
	
	//TODO: CascadeType不可使用到remove
	// 我一人多
	// 串「vehicle」
	@OneToMany(mappedBy = "location", cascade = CascadeType.ALL)
	@JsonIgnore
	public List<Vehicle> vehicles;
	
	// 串「dispatch_log」:from
	@OneToMany(mappedBy = "fromLocation", cascade = CascadeType.ALL)
	@JsonIgnore
	public List<DispatchLog> dispatchFroms;
	
	// 串「dispatch_log」:to
	@OneToMany(mappedBy = "toLocation", cascade = CascadeType.ALL)
	@JsonIgnore
	public List<DispatchLog> dispatchTos;
	
	// 串「cross_location_fee」:from
	@OneToMany(mappedBy = "fromLocation", cascade = CascadeType.ALL)
	@JsonIgnore
	public List<CrossLocationFee> crossLocationFeeFroms;
	
	// 串「cross_location_fee」:to
	@OneToMany(mappedBy = "toLocation", cascade = CascadeType.ALL)
	@JsonIgnore
	public List<CrossLocationFee> crossLocationFeeTos;
	
//	// 串「rental_order」:from
//	@OneToMany(mappedBy = "pickupLocation", cascade = CascadeType.ALL)
//	@JsonIgnore
//	public List<RentalOrderBean> pickupLocations;
//	
//	// 串「rental_order」:to
//	@OneToMany(mappedBy = "returnLocation", cascade = CascadeType.ALL)
//	@JsonIgnore
//	public List<RentalOrderBean> returnLocations;
	
//	// 串「viewing_appointment」
//	@OneToMany(mappedBy = "location", cascade = CascadeType.ALL)
//	@JsonIgnore
//	public List<ViewingAppointmentBean> viewingAppointmentBeans;
	
	
}
