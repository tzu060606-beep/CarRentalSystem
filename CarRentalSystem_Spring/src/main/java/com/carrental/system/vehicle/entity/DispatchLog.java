package com.carrental.system.vehicle.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.carrental.system.login.entity.DriverBean;
import com.carrental.system.login.entity.EmployeeBean;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity @Table(name = "dispatch_log")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE dispatch_log SET is_deleted = 1 WHERE dispatch_id = ?")
@SQLRestriction("is_deleted = 0")
public class DispatchLog {

	@Id @Column(name = "dispatch_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer dispatchId; // dispatch_id INT IDENTITY(1,1) NOT NULL, PK
	
	private LocalDateTime scheduledStartTime; // scheduled_start_time DATETIME2,
	private LocalDateTime actualStartTime; // actual_start_time DATETIME2,
	private LocalDateTime actualEndTime; // actual_end_time DATETIME2,
	private Integer startMileage; // start_mileage INT,
	private Integer endMileage; // end_mileage INT,
	private String reason; // reason NVARCHAR(200),
	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false;

	@Enumerated(EnumType.STRING)
	private DispatchStatus status; // status NVARCHAR(10), -- 待執行/調度中/已完成/已取消
	private String notes; // notes NVARCHAR(MAX),
	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt; // created_at DATETIME2,
	@LastModifiedDate
	private LocalDateTime updatedAt; // updated_at DATETIME2,
	
	@ManyToOne
	@JoinColumn(name = "from_location_id", referencedColumnName = "location_id")
	private Location fromLocation; // from_location_id INT, FK
	
	@ManyToOne
	@JoinColumn(name = "to_location_id", referencedColumnName = "location_id")
	private Location toLocation;// to_location_id INT, FK

	@ManyToOne
	@JoinColumn(name = "emp_id", referencedColumnName = "empId")
	private EmployeeBean employeeBean; // emp_id INT, FK
	
	@ManyToOne
	@JoinColumn(name = "driver_id", referencedColumnName = "driverId")
	private DriverBean driverBean;
	
	@ManyToOne
	@JoinColumn(name = "vehicle_id", referencedColumnName = "vehicle_id")
	private Vehicle vehicle; // vehicle_id INT, FK
}
