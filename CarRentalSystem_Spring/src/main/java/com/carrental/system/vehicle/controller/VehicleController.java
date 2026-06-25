package com.carrental.system.vehicle.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carrental.system.vehicle.dto.VehicleStatusUpdateRequest;
import com.carrental.system.vehicle.entity.CarModel;
import com.carrental.system.vehicle.entity.DispatchLog;
import com.carrental.system.vehicle.entity.Vehicle;
import com.carrental.system.vehicle.entity.VehicleStatus;
import com.carrental.system.vehicle.exception.CustomValidationException;
import com.carrental.system.vehicle.service.VehicleService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicle")
@CrossOrigin // 用來處理CORS(跨域資源共用)，會允許特定所有外部玉的網頁應用程式請求後端API(避開SOP)
public class VehicleController {

	private VehicleService vehicleService;

	@Autowired // 建構子注入(可略)
	public VehicleController(VehicleService vehicleService) {
		this.vehicleService = vehicleService;
	}

//	================ 一、對外 API ================

	// TODO: 補筆記
//	API 1-1: 給「租車系統」前端「看車」用，抓可用車輛。
//		  「搶車下單」請在自己後端controller的新增(或修改)方法呼叫我的service API: getAvailableVehicles() + isVehicleAvailableForBooking()
	@GetMapping("/available")
	public ResponseEntity<List<Vehicle>> getAvailableVehicles(
			@RequestParam("reqStartTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime reqStartTime,
			@RequestParam("reqEndTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime reqEndTime) {
		List<Vehicle> availableVehicles = vehicleService.getAvailableVehicles(reqStartTime, reqEndTime);
		return ResponseEntity.ok(availableVehicles);
	}
//	API 1-2: 給「專車系統」前端「看車」用，抓可用車輛。
//		  「搶車下單」請在自己後端controller的新增(或修改)方法呼叫我的service API: getAvailableVehicles() + isVehicleAvailableForBooking()
	@GetMapping("/available/transfer")
	public ResponseEntity<List<Vehicle>> getAvailableVehiclesForTransfer(
			@RequestParam("reqStartTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime reqStartTime,
			@RequestParam("reqEndTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime reqEndTime) {
		List<Vehicle> availableVehicles = vehicleService.getAvailableVehiclesForTransfer(reqStartTime, reqEndTime);
		return ResponseEntity.ok(availableVehicles);
	}

//	API 1-3: 給「異地調度」前端呼叫
	@GetMapping("/available/dispatch")
	public ResponseEntity<List<Vehicle>> getVehiclesReadyForDispatch(
			@RequestParam("reqStartTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime reqStartTime) {
		// 加這句，調度結束時間固定為「開始時間＋固定車程時間」
		LocalDateTime reqEndTime = reqStartTime.plusHours(VehicleService.DISPATCHED_FIXED_HOURS);
		List<Vehicle> availableVehicles = vehicleService.getAvailableVehicles(reqStartTime, reqEndTime);
		return ResponseEntity.ok(availableVehicles);
	}

//	==================== 二、CRUD ====================

//	查單筆
	@GetMapping("/{vehicleId}")
	public Vehicle getVehicle(@PathVariable("vehicleId") Integer vehicleId) {
		Vehicle vehicle = vehicleService.getVehicleById(vehicleId);
		return vehicle;
	}

//	查全部
	@GetMapping
	public List<Vehicle> listVehicles() {
		return vehicleService.getAllVehicles();
	}

//	增
	@PostMapping
	public Vehicle addVehicle(@RequestBody Vehicle vehicle) {
		// Map<>錯誤收集器
		Map<String, String> errors = new HashMap<String, String>();

		// 1. 必填：plateNo/issueDate/status/mileage
		requiredNotBlank(errors, "plateNo", vehicle.getPlateNo(), "車牌號碼為必填，不可空白");
		requiredNotNull(errors, "issueDate", vehicle.getIssuedDate(), "發照日期為必填，不可空白");
		requiredNotNull(errors, "status", vehicle.getStatus(), "車輛狀態為必填，不可空白");
		requiredNotNull(errors, "mileage", vehicle.getMileage(), "目前里程數為必填，不可空白");

		// 2. plateNo正則驗證
		if (!vehicle.getPlateNo().matches("^[A-Z]{3}-[0-9]{4}$")) {
			errors.put("plateNo", "車牌號碼限填「3碼字母-4碼數字」");
		}

		// ...

		// 檢查收集箱內是否有東西(錯誤)，若有直接丟給exception類別處理，不進到service層
		if (!errors.isEmpty()) {
			throw new CustomValidationException(errors);
		}
		// 確認驗證通過，才放行到service處理CRUD
		return vehicleService.createVehicle(vehicle);
	}

//	改
	@PutMapping("/{vehicleId}")
	public Vehicle editVehicle(@PathVariable("vehicleId") Integer vehicleId, @RequestBody Vehicle vehicle) {
		vehicle.setVehicleId(vehicleId);
		return vehicleService.updateVehicle(vehicleId, vehicle);
	}

//	刪
	@DeleteMapping("/{vehicleId}")
	public String removeVehicle(@PathVariable("vehicleId") Integer vehicleId) {
		Vehicle vehicle = vehicleService.getVehicleById(vehicleId);
		if (vehicle != null) {
			vehicleService.deleteVehicle(vehicleId);
			return "刪除成功";
		}
		return "查無此ID車輛，無法刪除";
	}

//	查單筆（含已軟刪除）
	@GetMapping("/all/{vehiclelId}")
	public Vehicle getVehicleIncludingDeleted(@PathVariable("vehiclelId") Integer vehicleId) {
		return vehicleService.getVehicleByIdIncludingDeleted(vehicleId);
	}

//	查全部「包含已軟刪除」
	@GetMapping("/all")
	public List<Vehicle> listVehiclesIncludingDeleted() {
		return vehicleService.getAllVehiclesIncludingDeleted();
	}

//	================ 三、本身狀態機＆特殊商業邏輯 ================

//	商業邏輯：狀態轉換機(State Machine)
	@PatchMapping("/{vehicleId}/status") // 部分修改
	public String updateVehicleStatus(
			@PathVariable("vehicleId") Integer vehicleId,
			@RequestBody VehicleStatusUpdateRequest request) {
		// 防呆驗證
		Map<String, String> errors = new HashMap<String, String>();
		requiredNotNull(errors, "newStatus", request.getNewStatus(), "車輛狀態不可空白");
		if (!errors.isEmpty()) {
			throw new CustomValidationException(errors);
		}
		// 驗證通過，放行service
		vehicleService.updateVehicleStatus(vehicleId, request.getNewStatus());
		return "狀態轉換成功";
	}

//	================ 四、私有輔助方法 ================

//	輔助方法：「字串」必填防空白
	private void requiredNotBlank(Map<String, String> errors, String fieldName, String value, String message) {
		if (value == null || value.trim().isEmpty()) {
			errors.put(fieldName, message);
		}
	}

//	輔助方法：「數字或物件」必填
	private void requiredNotNull(Map<String, String> errors, String fieldName, Object value, String message) {
		if (value == null) {
			errors.put(fieldName, message);
		}
	}

}
