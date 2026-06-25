package com.carrental.system.transfer.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.carrental.system.vehicle.entity.Vehicle;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@Table(name = "transfer_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id")
    private Integer transferId;

    @Column(name = "cust_id")
    private Integer custId;

    @Transient
    private String custName;

    @Column(name = "cust_phone", length = 20)
    private String custPhone;

    @Column(name = "driver_id")
    private Integer driverId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    @JsonIgnore // 避免把整坨車輛資料傳給前端
    private Vehicle vehicle;
    
    /**
     * 提供 JPQL 使用的 vehicleId 欄位
     * 不參與新增與修改，避免和 vehicle 關聯衝突
     */
    @Column(name = "vehicle_id", insertable = false, updatable = false)
    private Integer vehicleId;

    // =====================================================================

    @Column(name = "rate_id")
    private Integer rateId;

    @Column(name = "transfer_type", length = 50)
    private String transferType;

    @Column(name = "pickup_location", length = 255)
    private String pickupLocation;

    @Column(name = "dropoff_location", length = 255)
    private String dropoffLocation;

    @Column(name = "scheduled_pickup_time")
    private LocalDateTime scheduledPickupTime;

    @Column(name = "scheduled_dropoff_time")
    private LocalDateTime scheduledDropoffTime;

    @Column(name = "real_dropoff_time")
    private LocalDateTime realDropoffTime;

    @Column(name = "passenger_count")
    private Integer passengerCount;

    @Column(name = "start_mileage")
    private Integer startMileage;

    @Column(name = "end_mileage")
    private Integer endMileage;

    @Column(name = "luggage_count")
    private Integer luggageCount;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "note", columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
