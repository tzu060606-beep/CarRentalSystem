package com.carrental.system.point.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(name = "product")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

	@Id
	@Column(name = "product_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer productId;

	@NonNull
	@Column(name = "product_name")
	@NotBlank(message = "商品名稱為必填")
	private String productName;

	@Column(name = "description")
	private String description;

	@NonNull
	@Column(name = "points_required")
	@Min(value = 1, message = "所需點數至少為 1")
	private Integer pointsRequired;

	@NonNull
	@Column(name = "stock_quantity")
	@Min(value = 0, message = "庫存數量不可為負數")
	private Integer stockQuantity;

	@Column(name = "is_active")
	private Boolean isActive;

	@Column(name = "image_url")
	private String imageUrl;
	
	//TODO:欄位新增
	@Column(name = "category")
	private String category;

}
