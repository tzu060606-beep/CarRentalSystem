package com.carrental.system.point.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "points_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsRule {

	@Id
	@Column(name = "rule_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer ruleId;


	@Column(name = "rule_name")
	private String ruleName;


	@Column(name = "rule_key")
	private String ruleKey;

	// decimal型別不確定
	@Column(name = "ratio")
	private BigDecimal ratio;

	@Column(name = "description")
	private String description;

	// bit型別不確定
	@Column(name = "is_active")
	private Boolean isActive;

}
