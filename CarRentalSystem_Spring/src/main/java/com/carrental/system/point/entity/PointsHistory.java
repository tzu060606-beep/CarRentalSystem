package com.carrental.system.point.entity;

import java.time.LocalDateTime;

import com.carrental.system.point.enums.ChangeType;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "points_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsHistory {

	@Id
	@Column(name = "record_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer recordId;

	@Column(name = "cust_id", updatable = false)
	private Integer custId;

	@Column(name = "remain_points")
	private Integer remainPoints;

	@Enumerated(EnumType.STRING) // 讓jpa存英文字串而不是數字
	@Column(name = "change_type")
	private ChangeType changeType;

	@Column(name = "points_change")
	private Integer pointsChange;

	@Column(name = "reference_id", updatable = false)
	private String referenceId;

	@Column(name = "notes")
	private String notes;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@Column(name = "create_time", updatable = false)
	private LocalDateTime createTime;

	// TODO:欄位新增
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@Column(name = "expire_time", updatable = false)
	private LocalDateTime expireTime;
	
	// FIFO 扣點用：這筆獲得點數紀錄還有多少點數可以被扣除
	// 只有 pointsChange > 0 的紀錄才有值，扣點紀錄（REDEMPTION）為 null
	@Column(name = "available_points")
	private Integer availablePoints;

}
