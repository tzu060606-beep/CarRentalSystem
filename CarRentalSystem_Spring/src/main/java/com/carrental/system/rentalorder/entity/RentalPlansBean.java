package com.carrental.system.rentalorder.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "rental_plans")
@Getter 
@Setter 
@Builder
@NoArgsConstructor // JPA 必備的空建構子
@AllArgsConstructor // Builder 需要的全參數建構子
@EntityListeners(AuditingEntityListener.class) // 🌟 啟動這張表的自動時間監聽器
@SQLDelete(sql = "UPDATE rental_plans SET is_deleted = 1 WHERE plan_id = ?") // 軟刪除攔截
@SQLRestriction("is_deleted = 0") // 查詢時自動過濾已刪除方案
@ToString(exclude = "orders") 
public class RentalPlansBean {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Integer planId;                  // INT
    
    
    @Column(name = "plan_name", length = 50)
    private String planName;             // NVARCHAR(50)
    
    @Column(name = "plan_desc", length = 255)
    private String planDesc;             // NVARCHAR(255)

    @Column(name = "is_long_term")
    private boolean isLongTerm;          // BIT //0為長租 1為短租

    @Column(name = "applied_vehicle_type", length = 30)
    private String appliedVehicleType;  // 小型轎車/中型轎車/休旅車/廂型車/電動車
    
    @Column(name = "base_price")
    private BigDecimal basePrice;        // DECIMAL

    @Column(name = "overtime_fee")
    private BigDecimal overtimeFee;      // DECIMAL

    @Column(name = "mileage_limit")
    private Integer mileageLimit;        // INT

    @Column(name = "excess_mileage_fee")
    private BigDecimal excessMileageFee; // DECIMAL
    
    // 上架狀態
    @Column(name = "is_active")
    private boolean active; 
    
    // 軟刪除對應欄位
    @Column(name = "is_deleted")
    private boolean deleted;

    //自動注入時間欄位
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;     // DATETIME2
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;     // DATETIME2


    @OneToMany(mappedBy = "rentalPlan", fetch = FetchType.LAZY)
    @JsonIgnore
    @Transient
    private List<RentalOrderBean> orders; // 2. 必須改成 List 集合！
    

}