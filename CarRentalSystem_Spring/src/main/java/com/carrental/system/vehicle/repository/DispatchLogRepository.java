package com.carrental.system.vehicle.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.carrental.system.vehicle.entity.DispatchLog;
import com.carrental.system.vehicle.entity.DispatchStatus;

@Repository
public interface DispatchLogRepository extends JpaRepository<DispatchLog, Integer> {

//	查詢全部並分頁（可略，JpaRepository內建就有）
	Page<DispatchLog> findAll(Pageable pageable);
//	依狀態分類後查詢並分頁
	Page<DispatchLog> findByStatus(DispatchStatus status, Pageable pageable);
	
	long countByStatus(DispatchStatus status);
	
//	依vehicleId查詢
	@Query("SELECT d FROM DispatchLog d WHERE d.vehicle.vehicleId = :vehicleId")
	List<DispatchLog> findByVehicleId(@Param("vehicleId") Integer vehicleId);
}