package com.carrental.system.vehicle.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carrental.system.login.entity.DriverBean;
import com.carrental.system.login.repository.DriverRepository;
import com.carrental.system.vehicle.entity.DispatchLog;
import com.carrental.system.vehicle.entity.DispatchStatus;
import com.carrental.system.vehicle.entity.Location;
import com.carrental.system.vehicle.entity.Vehicle;
import com.carrental.system.vehicle.entity.VehicleStatus;
import com.carrental.system.vehicle.exception.CustomValidationException;
import com.carrental.system.vehicle.repository.DispatchLogRepository;
import com.carrental.system.vehicle.repository.LocationRepository;
import com.carrental.system.vehicle.repository.VehicleRepository;

@Service
@Transactional
public class DispatchLogService {
	
	private DispatchLogRepository dispatchLogRepository;
	private VehicleService vehicleService;
	private LocationRepository locationRepository;
	private DriverRepository driverRepository;
	
	@Autowired
	public DispatchLogService(DispatchLogRepository dispatchLogRepository, VehicleService vehicleService, LocationRepository locationRepository, DriverRepository driverRepository) {
		this.dispatchLogRepository = dispatchLogRepository;
		this.vehicleService = vehicleService;
		this.locationRepository = locationRepository;
		this.driverRepository = driverRepository;
	}
	
//	==================== 二、CRUD ====================
	
//	查單筆
	public DispatchLog getDispatchLogById(Integer dispatchId) {
		Optional<DispatchLog> dispatchLog = dispatchLogRepository.findById(dispatchId);
		if (dispatchLog.isPresent()) {
			return dispatchLog.get();
		}
		return null;
	}
	
//	查全部
	public List<DispatchLog> getAllDispatchLogs(){
		return dispatchLogRepository.findAll();
	}
	
//	分頁查詢(並可依調度狀態)
	public Page<DispatchLog> getDispatchLogs(DispatchStatus status, Pageable pageable){
		if (status == null) {
			return dispatchLogRepository.findAll(pageable); //若沒有特定狀態就查全部
		} else {
			return dispatchLogRepository.findByStatus(status, pageable);
		}
	}
	
//	by vehicleId查詢
	public List<DispatchLog> getDispatchByVehicleId(Integer vehicleId){
		if (vehicleId == null) {
			throw new IllegalArgumentException("車輛 ID 不可空白");
		}
		return dispatchLogRepository.findByVehicleId(vehicleId);
	}

	//	by status查詢
//	public DispatchStatus getDispatchCounts(DispatchStatus status){
//		dispatchLogRepository.countByStatus(status);
//	}
	
//	增
	public DispatchLog createDispatchLog(DispatchLog dispatchLog) {
		// 在save之前，幫所有關聯物件驗明正身
		// 1. 處理fromLocation
		if (dispatchLog.getFromLocation() != null && dispatchLog.getFromLocation().getLocationId() != null) {
			 // 利用locationRepository，去資料庫撈出原資料(本尊)
			 Location fromLoc = locationRepository.findById(dispatchLog.getFromLocation().getLocationId())
					 .orElseThrow(() -> new RuntimeException("查無出發據點"));
			 // 把查出來的本尊塞回，覆蓋前端傳來的野生實體
			 dispatchLog.setFromLocation(fromLoc);
		}
		// 2. 處理toLocation
		if (dispatchLog.getToLocation() != null && dispatchLog.getToLocation().getLocationId() != null) {
			Location toLoc = locationRepository.findById(dispatchLog.getToLocation().getLocationId())
					.orElseThrow(() -> new RuntimeException("查無目的據點"));
			dispatchLog.setToLocation(toLoc);
		}
		
		// 拿到完整車輛實體
		Integer vehicleId = dispatchLog.getVehicle().getVehicleId();
		Vehicle vehicle = vehicleService.getVehicleById(vehicleId);
		dispatchLog.setVehicle(vehicle);
		// 🌟人車合一：從車子身上拔出專屬司機
		DriverBean assignDriver = vehicle.getDriverBean();
		// 情況Ａ：有專屬司機
		if (assignDriver != null) {
			// 作為歷史存照寫入單據，以後車換人開，這張單依然有紀錄是誰跑的
			dispatchLog.setDriverBean(assignDriver);
		} else {
			// 情況Ｂ：無專屬司機，則檢查前端是否有「手動指派」司機並傳入後端
			if (dispatchLog.getDriverBean() != null && dispatchLog.getDriverBean().getDriverId() != null) {
				// 去DB查是否確有這位司機
				DriverBean manualDriver = driverRepository.findById(dispatchLog.getDriverBean().getDriverId())
						.orElseThrow(() -> new RuntimeException("查無此指派司機"));
				dispatchLog.setDriverBean(manualDriver);
			} else {
				// 情況Ｃ：若無專屬司機＋前端也沒指派，則報錯
				throwCustomValidationException("vehicle", "未指派司機，無法執行調度任務");
			}
		}
		// ======商業邏輯驗證======
		// 驗證1: 出發與目的據點不可相同
		if (dispatchLog.getFromLocation() != null && dispatchLog.getToLocation() != null) {
			if (dispatchLog.getFromLocation().getLocationId().equals(dispatchLog.getToLocation().getLocationId())) {
				throwCustomValidationException("toLocation", "出發與目的據點不可相同");
			}
		}
		// 驗證2: 預計出發時間不可早於當下
		if (dispatchLog.getScheduledStartTime() != null) {
			if (dispatchLog.getScheduledStartTime().isBefore(LocalDateTime.now())) {
				throwCustomValidationException("scheduledStartTime", "預計出發時間不可填寫過去時間");
			}
		}
		// =======================
		// 調度狀態自動帶「待執行」
		dispatchLog.setStatus(DispatchStatus.PENDING);
		// 存入資料庫產生調度單ID
		DispatchLog savedDispatchLog = dispatchLogRepository.save(dispatchLog);
		// 車輛狀態轉「待調度」刪除此狀態
//		vehicleService.updateVehicleStatus(savedDispatchLog.getVehicle().getVehicleId(), VehicleStatus.TOBE_DISPATCHED);
		return savedDispatchLog;
	}
	
//	改
	public DispatchLog updateDispatchLog(DispatchLog dispatchLog) {
		return dispatchLogRepository.save(dispatchLog);
	}
	
//	刪
	public boolean deleteDispatchLog(Integer dispatchId) {
		dispatchLogRepository.deleteById(dispatchId);
		return true;
	}
	
//	================ 三、本身特殊商業邏輯 ================
	
//	開始調度：狀態轉換＋更新「調度」起始里程、實際開始時間
	public void startDispatch(Integer dispatchId) {
		DispatchLog dispatchLog = dispatchLogRepository.findById(dispatchId).orElseThrow(() -> new RuntimeException("查無此ID對應調度單"));
		Vehicle vehicle = dispatchLog.getVehicle();
		// 將起始里程設為「車輛」目前里程、實際開始時間設為當下
		dispatchLog.setStartMileage(vehicle.getMileage());
		dispatchLog.setActualStartTime(LocalDateTime.now());
		// 調度狀態轉「執行中」
		dispatchLog.setStatus(DispatchStatus.IN_PROCESS);
		// 車輛狀態轉「調度中」
		//TODO:車必須在場內才可開始調度
		if ( vehicle.getStatus().getDbCode().equals("AVAILABLE") ) {
			vehicleService.updateVehicleStatus(dispatchLog.getVehicle().getVehicleId(), VehicleStatus.DISPATCHING);			
		} else {
			throwCustomValidationException("status", "該車輛不在場內，無法開始調度");
		}
		dispatchLogRepository.save(dispatchLog);
	}
	
//	完成調度：狀態轉換＋更新「車輛」里程
	public void finishDispatch(Integer dispatchId, Integer endMileage) {
		DispatchLog dispatchLog = dispatchLogRepository.findById(dispatchId).orElseThrow(() -> new RuntimeException("查無此ID對應調度單"));
		
		// 狀態機防呆：利用狀態機的 canTransitionTo方法做安全檢查
		if (!dispatchLog.getStatus().canTransitionTo(DispatchStatus.FINISHED)) {
			throwCustomValidationException("status", String.format("狀態轉換失敗：無法從[%s]轉換為[已結單]", dispatchLog.getStatus().getDescription()));
		}
		// 調度狀態轉「已完成」
		dispatchLog.setStatus(DispatchStatus.FINISHED);
		// 將前端回傳新里程設為「調度單結束里程數」，並記錄實際結案時間
		dispatchLog.setEndMileage(endMileage);
		dispatchLog.setActualEndTime(LocalDateTime.now());
		// 將調度單結束里程設為「車輛」里程數
		vehicleService.updateVehicleMileage(dispatchLog.getVehicle().getVehicleId(), endMileage);
		// 車輛狀態轉「場內(整理中)」
		vehicleService.updateVehicleStatus(dispatchLog.getVehicle().getVehicleId(), VehicleStatus.CLEANING);
		dispatchLogRepository.save(dispatchLog);
	}
	
//	取消調度：狀態轉換
	public void cancelDispatch(Integer dispatchId) {
		DispatchLog dispatchLog = dispatchLogRepository.findById(dispatchId).orElseThrow(() -> new RuntimeException("查無此ID對應調度單"));
		// 調度狀態轉「已取消」
		dispatchLog.setStatus(DispatchStatus.CANCEL);
		// 車輛狀態轉「場內(可出租)」
		vehicleService.updateVehicleStatus(dispatchLog.getVehicle().getVehicleId(), VehicleStatus.AVAILABLE);
		dispatchLogRepository.save(dispatchLog);
	}
	
	
//	================ 四、私有輔助方法 ================
//	輔助方法1：拋出CustomValidationException
	private void throwCustomValidationException(String fieldName, String errorString) {
		Map<String, String> errors = new HashMap<String, String>();
		errors.put(fieldName, errorString);
		throw new CustomValidationException(errors);
	}
}
