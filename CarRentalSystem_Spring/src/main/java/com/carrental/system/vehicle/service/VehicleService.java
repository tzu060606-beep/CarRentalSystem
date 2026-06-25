package com.carrental.system.vehicle.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carrental.system.vehicle.repository.VehicleRepository;
import com.carrental.system.vehicle.dto.VehicleStatusUpdateRequest;
import com.carrental.system.vehicle.entity.CarModel;
import com.carrental.system.vehicle.entity.Vehicle;
import com.carrental.system.vehicle.entity.VehicleStatus;
import com.carrental.system.vehicle.exception.CustomValidationException;

@Service
@Transactional
public class VehicleService {

	private VehicleRepository vehicleRepository;

	@Autowired //建構子注入
	public VehicleService(VehicleRepository vehicleRepository) {
		this.vehicleRepository = vehicleRepository;
	}
	
//	================ 一、對外 API ================
	
//	全域常數(for getAvailableVehicles)
	private static final int BUFFER_HOURS = 2; //洗車整理緩衝時間
	public static final int DISPATCHED_FIXED_HOURS = 2; //寫死調度車程時間2小時
	
//	API 1-1：建單時抓取指定時段內可用車輛 (for租車/調度)，務必搭配API2使用！！！
	public List<Vehicle> getAvailableVehicles(LocalDateTime reqStartTime, LocalDateTime reqEndTime){
		// (1)基礎防呆：新單下車時間不可早於上車時間
		if (reqEndTime.isBefore(reqStartTime)) {
			throwCustomValidationException("time", "下車時間不可早於上車時間");
		}
		// 🌟(2)計算防撞區間的起點
		//		往前扣 「整理時間」，現設為2小時
		//		往前扣 「整理時間＋調度時長」，現設為2+2=4小時
		LocalDateTime reqStartTimeWithCleaning = reqStartTime.minusHours(BUFFER_HOURS);
		LocalDateTime reqStartTimeOfDispatching = reqStartTime.minusHours(BUFFER_HOURS + DISPATCHED_FIXED_HOURS);
		
		// (3)計算好的時間丟給 VehicleRepository 執行過濾
		return vehicleRepository.findAvailableVehicles(reqEndTime, reqStartTimeWithCleaning, reqStartTimeOfDispatching);
	}
//	API 1-2：建單時抓取指定時段內可用車輛 (for專車)，務必搭配API2使用！！！
	public List<Vehicle> getAvailableVehiclesForTransfer(LocalDateTime reqStartTime, LocalDateTime reqEndTime){
		// (1)基礎防呆：新單下車時間不可早於上車時間
		if (reqEndTime.isBefore(reqStartTime)) {
			throwCustomValidationException("time", "下車時間不可早於上車時間");
		}
		// 🌟(2)計算防撞區間的起點
		//		往前扣 「整理時間」，現設為2小時
		//		往前扣 「整理時間＋調度時長」，現設為2+2=4小時
		LocalDateTime reqStartTimeWithCleaning = reqStartTime.minusHours(BUFFER_HOURS);
		LocalDateTime reqStartTimeOfDispatching = reqStartTime.minusHours(BUFFER_HOURS + DISPATCHED_FIXED_HOURS);
		
		// (3)計算好的時間丟給 VehicleRepository 執行過濾
		return vehicleRepository.findAvailableVehiclesForTransfer(reqEndTime, reqStartTimeWithCleaning, reqStartTimeOfDispatching);
	}
	
//	API 2：後端雙重驗證＆併發控制 (避免下單時間差造成超賣)
	public void checkAvailableForBooking(Integer vehicleId, LocalDateTime reqStarTime, LocalDateTime reqEndTime) {
		// 1. 基礎防呆：
		if (reqEndTime.isBefore(reqStarTime)) {
			throwCustomValidationException("time", "下車時間不可早於上車時間");
		}
		// 2. 車輛本體檢查
		// (1)車輛不存在(null)
		Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow(() -> new RuntimeException("查無該車輛資料"));
		// (2)車輛已軟刪除
		if (vehicle.getIsDeleted()) {
			throwCustomValidationException("vehicle", "該車輛已下架，請重新選擇");
		}
		// (3)車輛狀態為「退役」或「維修中」
		if (vehicle.getStatus().name().equals("RETIRED") ||
			vehicle.getStatus().name().equals("MAINTAINING") ||
			vehicle.getStatus().name().equals("TOBEMAINTAINED")) {
			throwCustomValidationException("vehicle", "該車輛目前狀態為【" + vehicle.getStatus().getDescription() + "】，無法預約");
		}
		// 3. 計算防撞區間的起點🌟
		LocalDateTime reqStartTimeWithCleaning = reqStarTime.minusHours(BUFFER_HOURS); //6
		LocalDateTime reqStartTimeOfDispatching = reqStarTime.minusHours(BUFFER_HOURS + DISPATCHED_FIXED_HOURS); //4
		
		// 4. 檢查並丟出「撞到租車訂單」錯誤訊息
		if (vehicleRepository.conflictRentalOrders(vehicleId, reqEndTime, reqStartTimeWithCleaning) > 0) {
			throwCustomValidationException("vehicle", "慢了一步！該時段已有【租車訂單】預約，請重新選擇");
		}
		// 5. 檢查並丟出「撞到專車訂單」錯誤訊息
		if (vehicleRepository.conflictTransferOrders(vehicleId, reqEndTime, reqStartTimeWithCleaning) > 0) {
			throwCustomValidationException("vehicle", "慢了一步！該時段已有【專車接送訂單】預約，請重新選擇");
		}
		// 6. 檢查並丟出「撞到調度單」錯誤訊息
		if (vehicleRepository.conflictDipatchLogs(vehicleId, reqEndTime, reqStartTimeOfDispatching) > 0) {
			throwCustomValidationException("vehicle", "Sorry！該時段此車輛已安排【異地據點調度】，請重新選擇");
		}
	}
	
//	API 3：狀態連動 兼 本身狀態轉換機
//	商業邏輯：狀態轉換機(State Machine)
	public void updateVehicleStatus(Integer vehicleId, VehicleStatus newStatus) {
		
		// 取現有舊實體(同時確認該ID有對應實體)
		Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow(() -> new RuntimeException("查無此ID對應車輛"));
		
		// 小整理變數名
		VehicleStatus currentStatus = vehicle.getStatus(); //舊狀態(物件)
		
		// 核心：狀態轉換驗證機(驗證順序無誤)
		if (!currentStatus.canTransitionTo(newStatus)) {
			throwCustomValidationException("status", String.format("狀態轉換失敗：無法從[%s]轉換至[%s]", currentStatus.getDescription(), newStatus.getDescription()));
		}
		// 以上皆通過後，更新實體狀態並存檔
		vehicle.setStatus(newStatus);
		vehicleRepository.save(vehicle);
	}
	
//	API 4：還車(RETURNED)/專車訂單完成(已完成)/ 調度完成(FINISHED) 更新里程
	public void updateVehicleMileage(Integer vehicleId, Integer newMileage) {
		if (newMileage == null) {
			throwCustomValidationException("mileage", "必須輸入最新里程數");
		}
		Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow(() -> new RuntimeException("查無此ID對應車輛"));
		// 核心驗證：新里程不可少於原里程
		if (newMileage <= vehicle.getMileage()) {
			throwCustomValidationException("mileage", "輸入里程數不可小於原里程數：" + vehicle.getMileage());
		}
		// 驗證通過，存入新里程數
		vehicle.setMileage(newMileage);
		vehicleRepository.save(vehicle);
	}
	
//	==================== 二、CRUD ====================
	
//	查單筆
	public Vehicle getVehicleById(Integer vehicleId) {
		Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow(() -> new RuntimeException("查無此ID對應車輛"));
		return vehicle;
	}
	
//	查全部
	public List<Vehicle> getAllVehicles(){
		return vehicleRepository.findAll();
	}
	
//	增
	public Vehicle createVehicle(Vehicle vehicle) {
		// 呼叫repository的existsByPlateNo方法確認是否已有該車牌號碼
		if (vehicleRepository.existsByPlateNo(vehicle.getPlateNo())) {
			throw new IllegalArgumentException("此車牌號碼已存在，無法新增");
		}
		Vehicle savedVehicle = vehicleRepository.save(vehicle);
		return savedVehicle;
//		return vehicleRepository.findById(savedVehicle.getVehicleId()).orElse(savedVehicle);
	}
	
//	改
	public Vehicle updateVehicle(Integer vehicleId, Vehicle vehicle) {
		// 拉出DB中該車舊資料
		Optional<Vehicle> existingVehicle = vehicleRepository.findById(vehicleId);
		if (existingVehicle.isEmpty()) {
			throw new RuntimeException("查無此ID對應車輛，無法修改");
		}
		// 強制將前端status輸入框傳回的值，以現有舊status的值覆蓋
		Vehicle originVehicle = existingVehicle.get();
		vehicle.setStatus(originVehicle.getStatus());
		
		//確保留下相同ID後，覆寫存檔
		vehicle.setVehicleId(vehicleId);
		return vehicleRepository.save(vehicle);
	}
	
//	刪
	public boolean deleteVehicle(Integer vehicleId) {
		vehicleRepository.deleteById(vehicleId);
		return true;
	}
	
//	查單筆（含已軟刪除）
	public Vehicle getVehicleByIdIncludingDeleted(Integer vehicleId) {
		Optional<Vehicle> selectedVehicle = vehicleRepository.findByIdIncludingDeleted(vehicleId);
		if (selectedVehicle.isPresent()) {
			return selectedVehicle.get();
		}
		return null;
	}
	
//	查全部（含已軟刪除）
	public List<Vehicle> getAllVehiclesIncludingDeleted(){
		return vehicleRepository.findAllIncludingDeleted();
	}

//	================ 三、本身商業邏輯 ================
	
//	商業邏輯：狀態轉換機(State Machine)
//	public  Vehicle updateVehicleStatus(Integer vehicleId, VehicleStatusUpdateRequest request) {
//		
//		// 取現有舊實體(同時確認該ID有對應實體)
//		Optional<Vehicle> selectedVehicle = vehicleRepository.findById(vehicleId);
//		if (selectedVehicle.isEmpty()) {
//			throw new RuntimeException("找不到此ID[" + vehicleId + "]對應車輛");
//		} 
//		
//		// 小整理變數名
//		Vehicle vehicle = selectedVehicle.get(); //舊實體
//		VehicleStatus currentStatus = vehicle.getStatus(); //舊狀態(物件)
//		VehicleStatus newStatus = request.getNewStatus(); //新狀態(物件)
//		
//		// 核心：狀態轉換驗證機(驗證順序無誤)
//		if (!vehicle.getStatus().canTransitionTo(newStatus)) {
//			Map<String, String> errors = new HashMap<String, String>();
//			String errorString = String.format("狀態轉換失敗：無法從[%s]轉換至[%s]", currentStatus.getDescription(), newStatus.getDescription());
//			errors.put("status", errorString);
//			throw new CustomValidationException(errors);
//		}
//		
//		// 副作用：針對不同狀態附帶的變動
//		switch (newStatus) {
//		
//			case CLEANING:
//				// 回場：更新里程數(from租車/專車/調度)
//				if (currentStatus == VehicleStatus.RENTING || //租車訂單狀態：CLOSED 
//					currentStatus == VehicleStatus.SHUTTLING || //專車訂單狀態：已完成
//					currentStatus == VehicleStatus.DISPATCHING) { //調度單狀態：FINISHED
//					
//					if (request.getCurrentMileage() == null) {
//						throwCustomValidationException("currentMileage", "車輛回場整理時，必須輸入最新里程數");
//					}
//					if (request.getCurrentMileage() < vehicle.getMileage()) {
//						String errorString = "輸入里程數不可小於原里程數：" + vehicle.getMileage();
//						throwCustomValidationException("currentMileage", errorString);
//					}
//					vehicle.setMileage(request.getCurrentMileage());
//				} else {
//					// 場內狀態互轉(如 可租借 -> 整理中)：里程數可不填，但不可小於原里程數
//					if (request.getCurrentMileage() < vehicle.getMileage()) {
//						String errorString = "輸入里程數不可小於原里程數：" + vehicle.getMileage();
//						throwCustomValidationException("currentMileage", errorString);
//					}
//					vehicle.setMileage(request.getCurrentMileage());
//				}
//				break;
//				
//			case AVAILABLE:
//				//TODO: 轉自 租車訂單狀態：CANCELLED / 專車訂單狀態：已取消 / 調度單狀態：CANCEL
//				break;	
//				
//			case RENTING:
//				//TODO: 轉自 租車取車 -> 1. 訂單狀態：PICKED_UP 2. rental起始里程數帶入vehicle目前里程數
//				break;
//				
//			case SHUTTLING:
//				//TODO: 轉自 專車出車 -> 1. 訂單狀態：已出車(目前沒設) 2. transfer起始里程數帶入vehicle目前里程數
//				break;
//				
//			case DISPATCHING:
//				//TODO: 轉自 調度出車 -> 1. 調度狀態：IN_PROCESS 2. dispatch起始里程數帶入vehicle目前里程數
//				break;
//				
//			case TOBEDISPATCHED:
//				if (request.getDipatchLogId() == null) {
//				throwCustomValidationException("dipatchLogId", "請指定調度單ID，若無請先建立調度單");
//				}
//				// TODO: dispatch_log enum更新方法
//			dispatchLogService.updateDispatchLog();
//				break;
//		}
//		
//		// 以上皆通過後，更新實體狀態並存檔
//		vehicle.setStatus(newStatus);
//		return vehicleRepository.save(vehicle);
//	}
	
//	================ 四、私有輔助方法 ================
	
//	輔助方法1：拋出CustomValidationException
	private void throwCustomValidationException(String fieldName, String errorString) {
		Map<String, String> errors = new HashMap<String, String>();
		errors.put(fieldName, errorString);
		throw new CustomValidationException(errors);
	}
	
}
