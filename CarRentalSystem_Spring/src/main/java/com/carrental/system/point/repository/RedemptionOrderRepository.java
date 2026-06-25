package com.carrental.system.point.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carrental.system.point.entity.RedemptionOrder;

public interface RedemptionOrderRepository extends JpaRepository<RedemptionOrder, Integer> {

	// 查詢兌換訂單關鍵字
	@Query("SELECT r FROM RedemptionOrder r WHERE r.customerBean.custName LIKE CONCAT( '%',:keyword,'%') OR r.product.productName LIKE CONCAT( '%',:keyword,'%')")
	List<RedemptionOrder> findByKeyword(@Param("keyword") String keyword);

	// 依訂單狀態篩選兌換訂單
	List<RedemptionOrder> findByOrderStatus(String orderStatus);
	
	// 查全部訂單，同時帶出客戶姓名和商品名稱
	// 【為什麼用 @Query】需要 JOIN 三張表（orders、customer、product），
	// 命名方法無法表達這麼複雜的查詢
	@Query("SELECT o FROM RedemptionOrder o " +
	       "JOIN FETCH o.customerBean " +
	       "JOIN FETCH o.product " +
	       "ORDER BY o.createTime DESC")
	List<RedemptionOrder> findAllWithDetails();
	// JOIN FETCH 是什麼？
	// 一般的 @ManyToOne 預設是 LAZY，查訂單時不會自動查客戶和商品，要用到時才查（N+1 問題）。
	// JOIN FETCH 強制一次 SQL 就把關聯的資料一起查出來，避免 N+1。

}
