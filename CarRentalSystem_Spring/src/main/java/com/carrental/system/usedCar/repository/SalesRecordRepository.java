package com.carrental.system.usedCar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.carrental.system.usedCar.entity.SalesRecordBean;

public interface SalesRecordRepository extends JpaRepository<SalesRecordBean, Integer> {

	// 透過 Customer 的 ID 查詢關聯的預約
	List<SalesRecordBean> findByCustomerCustId(Integer custId);
	
	// 新增：金流回傳後更新狀態
    @Transactional
    @Modifying
    @Query("UPDATE SalesRecordBean s SET s.payStatus = 'PAID' WHERE s.saleId = :id")
    void updateStatusToPaid(Integer id);
}
