package com.carrental.system.vehicle.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.convert.Jsr310Converters.LocalDateTimeToInstantConverter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.carrental.system.vehicle.entity.Vehicle;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {

	boolean existsByPlateNo(String platNo);
	
//	查單筆「包含已軟刪除」
	@Query(value = "SELECT * FROM vehicle WHERE vehicle_id = ?1", nativeQuery = true)
	Optional<Vehicle> findByIdIncludingDeleted(Integer vehicleId);
	
//	查全部「包含已軟刪除」
	@Query(value = "SELECT * FROM Vehicle", nativeQuery = true)
	List<Vehicle> findAllIncludingDeleted();
	
	// TODO: 加上自己寫的二層查詢，利用 SQL語法 或 DTO 取自己要的欄位
	@Query("SELECT v FROM Vehicle v JOIN FETCH v.carModel WHERE v.status = :status")
	List<Vehicle> findAvailableVehiclesWithModel(@Param("status") String status);

	@Query("SELECT v FROM Vehicle v JOIN FETCH v.location WHERE v.status = :status")
	List<Vehicle> findAvailableVehiclesWithLocation(@Param("status") String status);
	
//	以下全部都是「預計 (Expected/Scheduled/Requested)」時間！
//	為什麼？因為我們現在做的是「預測未來」。要判斷下個禮拜或明天車子能不能租，系統只能拿「大家計畫好的時間」來互相比對，不可能用到「實際發生」的時間（因為事情還沒發生）。
//	我們把這些變數拆成兩軍對陣，這樣看最清楚：
//
//	🟢 守備方：資料庫裡「別人已經佔用的時間」(舊單)
//	這些是已經存在資料庫中，會阻擋你借車的時間：
//
//	pickupTime (RentalOrder)： 別人租車的預計取車時間。（雖然欄位名稱沒特別寫 expected，但在客戶還沒真的一手交錢一手交鑰匙前，這就是他在網頁上選的預約起點）。
//	returnTime (RentalOrder)： 別人租車的預計還車時間。
//	scheduledStartTime (DispatchLog)： 你們排定的預計調度發車時間。
//
//	🔵 進攻方：API 傳入的「你現在想預約的時間」(新請求)
//	這些是前端或 Service 算好後，傳進來試圖尋找空檔的參數：
//
//	reqEndTime： 你這筆新訂單（或新調度）的預計結束時間。
//	reqStartTimeWithCleaning： 你這筆新訂單的預計開始時間，再往前扣除 2 小時（用來把前一手的洗車緩衝期算進去）。
//	reqStartTimeOfDispatching： 你這筆新訂單的預計開始時間，再往前扣除 6 小時（專門用來閃避調度的 4 小時車程 + 2 小時洗車）。
	
	
//	抓取「租車訂單」和「調度單」可用車輛
	@Query("SELECT v FROM Vehicle v WHERE v.isDeleted = false "
			+ "AND v.status NOT IN ('RETIRED', 'MAINTAINING') "
			
			// 1. 排除【RentalOrderBean (租車單)】撞期的車輛
			+ "AND NOT EXISTS (SELECT 1 FROM RentalOrderBean r WHERE r.vehicle.vehicleId = v.vehicleId " 
							+ "AND r.orderStatus NOT IN ('CLOSED', 'CANCELLED') "
							+ "AND r.pickupTime < :reqEndTime AND r.returnTime > :reqStartTimeWithCleaning) "
			// 2. 排除【你的 DispatchLog (調度單)】撞期的車輛 (用固定時長推算的危險區間)
			+ "AND NOT EXISTS (SELECT 1 FROM DispatchLog d WHERE d.vehicle.vehicleId = v.vehicleId "
							+ "AND d.status NOT IN ('FINISHED', 'CANCEL') "
							+ "AND d.scheduledStartTime < :reqEndTime AND d.scheduledStartTime > :reqStartTimeOfDispatching) "
			// 3. 排除【TransferOrder (專車接送單)】撞期的車輛				
			+ "AND NOT EXISTS (SELECT 1 FROM TransferOrder t WHERE t.vehicleId = v.vehicleId " //TODO: 後續若對方有加@ManyToOne，改為t.vehicle.vehicleId
							+ "AND t.status NOT IN ('已取消', '已完成') "  //TODO: 後續若對方有加enum英文代碼，改為英文代碼
							+ "AND t.scheduledPickupTime <: reqEndTime AND t.scheduledDropoffTime > :reqStartTimeWithCleaning)")
	List<Vehicle> findAvailableVehicles(
			@Param("reqEndTime") LocalDateTime reqEndTime, 
			@Param("reqStartTimeWithCleaning") LocalDateTime reqStartTimeWithCleaning,
			@Param("reqStartTimeOfDispatching") LocalDateTime reqStartTimeOfDispatching);
	
//	抓取「專車訂單」可用車輛
	@Query("SELECT v FROM Vehicle v WHERE v.isDeleted = false "
			+ "AND v.status NOT IN ('RETIRED', 'MAINTAINING') "
			+ "AND EXISTS (SELECT 1 FROM DriverBean d WHERE d.vehicle.vehicleId = v.vehicleId) "
			
			// 1. 排除【RentalOrderBean (租車單)】撞期的車輛
			+ "AND NOT EXISTS (SELECT 1 FROM RentalOrderBean r WHERE r.vehicle.vehicleId = v.vehicleId " 
			+ "AND r.orderStatus NOT IN ('CLOSED', 'CANCELLED') "
			+ "AND r.pickupTime < :reqEndTime AND r.returnTime > :reqStartTimeWithCleaning) "
			// 2. 排除【你的 DispatchLog (調度單)】撞期的車輛 (用固定時長推算的危險區間)
			+ "AND NOT EXISTS (SELECT 1 FROM DispatchLog d WHERE d.vehicle.vehicleId = v.vehicleId "
			+ "AND d.status NOT IN ('FINISHED', 'CANCEL') "
			+ "AND d.scheduledStartTime < :reqEndTime AND d.scheduledStartTime > :reqStartTimeOfDispatching) "
			// 3. 排除【TransferOrder (專車接送單)】撞期的車輛				
			+ "AND NOT EXISTS (SELECT 1 FROM TransferOrder t WHERE t.vehicleId = v.vehicleId " //TODO: 後續若對方有加@ManyToOne，改為t.vehicle.vehicleId
			+ "AND t.status NOT IN ('已取消', '已完成') "  //TODO: 後續若對方有加enum英文代碼，改為英文代碼
			+ "AND t.scheduledPickupTime <: reqEndTime AND t.scheduledDropoffTime > :reqStartTimeWithCleaning)")
	List<Vehicle> findAvailableVehiclesForTransfer(
			@Param("reqEndTime") LocalDateTime reqEndTime, 
			@Param("reqStartTimeWithCleaning") LocalDateTime reqStartTimeWithCleaning,
			@Param("reqStartTimeOfDispatching") LocalDateTime reqStartTimeOfDispatching);
	
//	確認單一特定車輛可否租用/專車/調度
	@Query("SELECT COUNT(v) FROM Vehicle v WHERE v.vehicleId = :vehicleId "
			+ "AND v.isDeleted = false AND v.status NOT IN ('RETIRED','MAINTAINING') "
			// 1. 排除【RentalOrderBean (租車單)】撞期的車輛
			+ "AND NOT EXISTS (SELECT 1 FROM RentalOrderBean r WHERE r.vehicle.vehicleId = v.vehicleId "
							+ "AND r.orderStatus NOT IN ('CLOSED', 'CANCELLED') "
							+ "AND r.pickupTime < :reqEndTime AND r.returnTime > :reqStartTimeWithCleaning) "
			// 2. 排除【你的 DispatchLog (調度單)】撞期的車輛
			+ "AND NOT EXISTS (SELECT 1 FROM DispatchLog d WHERE d.vehicle.vehicleId = v.vehicleId "
							+ "AND d.status NOT IN ('FINISHED', 'CANCEL') "
																		  // 現有結束時間 晚於 新單開始時間-2
																		  // 現有結束時間-2 晚於 新單開始時間-4
							+ "AND d.scheduledStartTime < :reqEndTime AND d.scheduledStartTime > :reqStartTimeOfDispatching) "
			// 3. 排除【TransferOrder (專車接送單)】撞期的車輛
			+ "AND NOT EXISTS (SELECT 1 FROM TransferOrder t WHERE t.vehicleId = v.vehicleId "
							+ "AND t.status NOT IN ('已取消', '已完成') "   //TODO: 後續若對方有加enum英文代碼，改為英文代碼
							+ "AND t.scheduledPickupTime < :reqEndTime AND t.scheduledDropoffTime > :reqStartTimeWithCleaning)")  
	long checkSingleVehicleAvailability(
			@Param("vehicleId") Integer vehicleId,
			@Param("reqEndTime") LocalDateTime reqEndTime, //10
			@Param("reqStartTimeWithCleaning") LocalDateTime reqStartTimeWithCleaning, //6
			@Param("reqStartTimeOfDispatching") LocalDateTime reqStartTimeOfDispatching); //4
	
//	細分1：單一特定車輛有撞租車？
	@Query("SELECT COUNT(r) FROM RentalOrderBean r WHERE r.vehicle.vehicleId = :vehicleId "
			+ "AND r.orderStatus NOT IN ('CLOSED', 'CANCELLED') "
			+ "AND r.pickupTime < :reqEndTime AND r.returnTime > :reqStartTimeWithCleaning")
	long conflictRentalOrders(
			@Param("vehicleId") Integer vehicleId,
			@Param("reqEndTime") LocalDateTime reqEndTime,
			@Param("reqStartTimeWithCleaning") LocalDateTime reqStartTimeWithCleaning);
	
//	細分2：單一特定車輛有撞專車？
	@Query("SELECT COUNT(t) FROM TransferOrder t WHERE t.vehicleId = :vehicleId "  //TODO: 後續若對方有加@ManyToOne，改為t.vehicle.vehicleId
			+ "AND t.status NOT IN ('已取消', '已完成') "   //TODO: 後續若對方有加enum英文代碼，改為英文代) 
			+ "AND t.scheduledPickupTime < :reqEndTime AND t.scheduledDropoffTime > :reqStartTimeWithCleaning")  
	long conflictTransferOrders(
			@Param("vehicleId") Integer vehicleId,
			@Param("reqEndTime") LocalDateTime reqEndTime,
			@Param("reqStartTimeWithCleaning") LocalDateTime reqStartTimeWithCleaning);
	
//	細分3：單一特定車輛有撞調度？
	@Query("SELECT COUNT(d) FROM DispatchLog d WHERE d.vehicle.vehicleId = :vehicleId "
			+ "AND d.status NOT IN ('FINISHED', 'CANCEL') "
			+ "AND  d.scheduledStartTime < :reqEndTime AND d.scheduledStartTime > :reqStartTimeOfDispatching")
	long conflictDipatchLogs(
			@Param("vehicleId") Integer vehicleId,
			@Param("reqEndTime") LocalDateTime reqEndTime,
			@Param("reqStartTimeOfDispatching") LocalDateTime reqStartTimeOfDispatching);
}
