package com.carrental.system.vehicle.entity;


import java.math.BigDecimal;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity @Table(name = "cross_location_fee")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
//@RequiredArgsConstructor
@SQLDelete(sql = "UPDATE cross_location_fee SET is_deleted = 1 WHERE fee_id = ?")
@SQLRestriction("is_deleted = 0")
public class CrossLocationFee {

	@Id @Column(name = "fee_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer feeId; // fee_id INT IDENTITY(1,1) NOT NULL,
	private BigDecimal extraFee; // extra_fee DECIMAL(10,2),
	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false;
	
	// 人一我多
	// 串「location」
	@ManyToOne //(fetch = FetchType.LAZY)
	@JoinColumn(name = "from_location_id", referencedColumnName = "location_id")
	private Location fromLocation; // from_location_id INT,
	
	// 串「location」
	@ManyToOne //(fetch = FetchType.LAZY)
	@JoinColumn(name = "to_location_id", referencedColumnName = "location_id")
	private Location toLocation; // to_location_id INT,
}
