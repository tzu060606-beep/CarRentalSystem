package com.carrental.system.usedCar.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carrental.system.usedCar.entity.ViewingAppointmentBean;

public interface ViewingAppointmentRepository extends JpaRepository<ViewingAppointmentBean, Integer> {

	// 檢查是否有衝突的預約
	// 邏輯：檢查該車是否有「狀態有效」且「時間在目標預約前後 1 小時內」的單據
	@Query("SELECT COUNT(v) > 0 FROM ViewingAppointmentBean v " + "WHERE v.usedCar.usedCarId = :usedCarId "
			+ "AND v.status NOT IN (com.carrental.system.usedCar.entity.ViewingAppointmentStatus.CANCELLED) "
			+ "AND v.apptTime BETWEEN :startTime AND :endTime")
	boolean isTimeSlotOccupied(@Param("usedCarId") Integer usedCarId, @Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime);

	// 透過 Customer 的 ID 查詢關聯的預約
	List<ViewingAppointmentBean> findByCustomerCustId(Integer custId);

	/**
     * 新增：透過車輛 ID 尋找該車輛「所有尚未結束」的預約單
     * 邏輯：撈出同車，且狀態是 PENDING (待確認) 或 CONFIRMED (已預定) 的單據
     */
    @Query("FROM ViewingAppointmentBean v " +
           "WHERE v.usedCar.usedCarId = :usedCarId " +
           "AND v.status IN (com.carrental.system.usedCar.entity.ViewingAppointmentStatus.PENDING, " +
           "                 com.carrental.system.usedCar.entity.ViewingAppointmentStatus.CONFIRMED)")
    List<ViewingAppointmentBean> findActiveAppointmentsByCarId(@Param("usedCarId") Integer usedCarId);
	
	/*
	 * 依據【預約看車時間】計算某筆預約單前面還有幾個人在排隊
	 * 邏輯：計算同車、未取消/未完成，且預約看車時間（apptTime）比當前傳入時間還要早的有效預約筆數
	 */
	@Query("SELECT COUNT(v) FROM ViewingAppointmentBean v " + "WHERE v.usedCar.usedCarId = :usedCarId "
			+ "AND v.status NOT IN (com.carrental.system.usedCar.entity.ViewingAppointmentStatus.CANCELLED, "
			+ "                     com.carrental.system.usedCar.entity.ViewingAppointmentStatus.COMPLETED) "
			+ "AND v.apptTime < :currentApptTime")
	long countPeopleAheadByApptTime(@Param("usedCarId") Integer usedCarId,
			@Param("currentApptTime") LocalDateTime currentApptTime);
}
