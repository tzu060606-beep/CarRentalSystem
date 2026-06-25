package com.carrental.system.point.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.carrental.system.point.dto.PointsHistoryResponseDTO;
import com.carrental.system.point.entity.PointsHistory;
import com.carrental.system.point.enums.ChangeType;

public interface PointsHistoryRepository extends JpaRepository<PointsHistory, Integer> {
	
	
	// 查全部（帶姓名）
	@Query("SELECT new com.carrental.system.point.dto.PointsHistoryResponseDTO(" +
	       "p.recordId, p.custId, c.custName, p.remainPoints, " +
	       "p.changeType, p.pointsChange, p.referenceId, p.notes, p.createTime, p.expireTime, p.availablePoints) " +
	       "FROM PointsHistory p JOIN CustomerBean c ON p.custId = c.custId " +
	       "ORDER BY p.createTime DESC")
	List<PointsHistoryResponseDTO> findAllWithCustName();

	// 查特定客戶（帶姓名）
	@Query("SELECT new com.carrental.system.point.dto.PointsHistoryResponseDTO(" +
	       "p.recordId, p.custId, c.custName, p.remainPoints, " +
	       "p.changeType, p.pointsChange, p.referenceId, p.notes, p.createTime, p.expireTime, p.availablePoints) " +
	       "FROM PointsHistory p JOIN CustomerBean c ON p.custId = c.custId " +
	       "WHERE p.custId = :custId " +
	       "ORDER BY p.createTime DESC")
	List<PointsHistoryResponseDTO> findByCustIdWithCustName(@Param("custId") Integer custId);

	// 查詢規則關鍵字
	@Query("SELECT p FROM PointsHistory p WHERE p.changeType LIKE CONCAT( '%',:keyword,'%') OR p.notes LIKE CONCAT( '%',:keyword,'%')")
	List<PointsHistory> findByKeyword(@Param("keyword") String keyword);

	// 確認referenceid和changeType是否存在
	Boolean existsByReferenceIdAndChangeType(String referenceId, ChangeType changeType);
	
	//確認custId和changeType是否存在
	boolean existsByCustIdAndChangeType(Integer custId, ChangeType changeType);

	//查詢客戶的點數異動紀錄(For F1)
	List<PointsHistory> findByCustId(Integer custId);
	
	
	// FIFO 扣點用：查詢某客戶所有「可用」的點數紀錄
	// 條件：custId 相同、availablePoints > 0、expireTime 還沒過期
	// 排序：createTime 升冪（最舊的先扣）
	@Query("SELECT p FROM PointsHistory p " +
		       "WHERE p.custId = :custId " +
		       "AND p.availablePoints > 0 " +
		       "AND p.expireTime > :now " +
		       "ORDER BY p.createTime ASC")
	List<PointsHistory> findAvailablePointsByFIFO(
	    @Param("custId") Integer custId, 
	    @Param("now") LocalDateTime now
	);
	
	
	// 排程用：查詢所有已過期但 availablePoints 還有值的紀錄
	// 條件：expireTime 已過、availablePoints > 0
	@Query("SELECT p FROM PointsHistory p " +
	       "WHERE p.expireTime <= :now " +
	       "AND p.availablePoints > 0")
	List<PointsHistory> findExpiredWithRemainingPoints(@Param("now") LocalDateTime now);
}