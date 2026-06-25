package com.carrental.system.rentalorder.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.carrental.system.rentalorder.entity.RentalOrderBean;
import com.carrental.system.rentalorder.enums.OrderStatus;



public interface RentalOrderRepository extends JpaRepository<RentalOrderBean, Integer>, JpaSpecificationExecutor<RentalOrderBean> {
    
	@Transactional
    @Modifying//告訴 Spring Data JPA 這不是 Select，不需要回傳結果集
    @Query(value = "UPDATE rental_order SET is_deleted = 0 WHERE order_id = :orderId", nativeQuery = true)
    int restoreOrder(@Param("orderId") Integer orderId);
    //@Param為橋樑，Java 變數值 ➔ @Param 標籤名稱 ➔ SQL 中的 :名稱。

    // 讓資料庫只幫你計算「符合條件的數量」
    // SQL 會是: SELECT COUNT(*) FROM rental_order WHERE cust_id = ? AND order_status = 'CLOSED'
    long countByCustomer_CustIdAndOrderStatus(Integer custId, OrderStatus orderStatus);

    //查某一筆訂單
    List<RentalOrderBean> findByCustomer_CustIdOrderByOrderTimeDesc(Integer custId);
    /*
    Customer_CustId：條件是「去找關聯的 customer 物件，看裡面的 custId 是否等於傳進來的參數
    這裡的底線 _ 很重要，它告訴系統：「這是一個巢狀關聯」，先找 Customer 再找裡面的 CustId)
    SELECT * FROM rental_order WHERE cust_id = ? ORDER BY order_time DESC
     */
    //查某會員的全部訂單
    Optional<RentalOrderBean> findByOrderIdAndCustomer_CustId(Integer orderId, Integer custId);
    /*
    SELECT * FROM rental_order WHERE order_id = ? AND cust_id = ?
     */
    
}
