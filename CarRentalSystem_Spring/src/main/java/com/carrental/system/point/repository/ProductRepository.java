package com.carrental.system.point.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carrental.system.point.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {

	// 查詢商品關鍵字
	@Query("SELECT p FROM Product p WHERE p.productName LIKE CONCAT('%', :keyword, '%') OR p.description LIKE CONCAT('%', :keyword, '%')")
	List<Product> findByKeyword(@Param("keyword") String keyword);

	// 篩選商品狀態、類別、排序商品-->之後建議用specification寫法
	// 所有篩選全部寫在同一支，不分散

	// 【A】第一段：status 篩選
	// 邏輯是「四選一」：
	// 1. 如果傳入 'all' → 第一個條件成立，整個括號為 true，所有商品都通過
	@Query("SELECT p FROM Product p WHERE " + "(" + "  :status = 'all' OR "
	// 2. 如果傳入 'active' → 只有 isActive=true 的商品通過
			+ "  (:status = 'active' AND p.isActive = true) OR "
			// 3. 如果傳入 'outofstock' → 只有 isActive=true 且 stockQuantity=0 的商品通過
			+ "  (:status = 'outofstock' AND p.isActive = true AND p.stockQuantity = 0) OR "
			// 4. 如果傳入 'inactive' → 只有 isActive=false 的商品通過
			+ "  (:status = 'inactive' AND p.isActive = false)" + ") AND "

			// 【B】第二段：category 篩選
			// 這段的邏輯是「有傳才篩」：
			// 如果沒傳 category（傳入 null）→ :category IS NULL 成立，所有商品都通過
			// 如果有傳 category → 只有 p.category = :category 的商品通過
			+ "(:category IS NULL OR p.category = :category) "

			// 【C】第三段：排序
			// CASE WHEN 是 SQL 的條件判斷，邏輯是：
			// 如果 sort = 'asc' → 第一行的 CASE WHEN 成立，回傳 pointsRequired，按低到高排
			+ "ORDER BY CASE WHEN :sort = 'asc' THEN p.pointsRequired END ASC, "
			// 如果 sort = 'desc' → 第二行的 CASE WHEN 成立，回傳 pointsRequired，按高到低排
			+ "         CASE WHEN :sort = 'desc' THEN p.pointsRequired END DESC")
	// 如果都不成立 → 兩個 CASE WHEN 都回傳 NULL，ORDER BY NULL 等於不排序
	List<Product> findByFilters(@Param("status") String status, @Param("category") String category,
			@Param("sort") String sort);

	
	
	
	
	
//	【所有篩選查詢排序統一合併到上方的方法中】	
//	// 查詢上架中商品(上架狀態為true且庫存>0)
//	@Query("SELECT p FROM Product p WHERE p.isActive = true AND p.stockQuantity>0")
//	List<Product> findActiveProducts();
//
//	// 查詢缺貨補貨中商品(上架狀態為true且庫存=0)
//	// List<Product> findByIsActiveTrueAndStockQuantityEquals(Integer
//	// stockQuantity);//jpa可以解析但語意太怪了
//	@Query("SELECT p FROM Product p WHERE p.isActive = true AND p.stockQuantity=0")
//	List<Product> findOutOfStockProducts();
//
//	// 查詢已下架商品(上架狀態為false)
//	List<Product> findByIsActiveFalse();
//
//	// 根據類別查詢商品
//	List<Product> findByCategory(String category);
}
