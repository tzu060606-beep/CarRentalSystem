package com.carrental.system.vehicle.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carrental.system.login.dto.DriverResponseDTO;
import com.carrental.system.login.dto.EmployeeResponseDTO;
import com.carrental.system.login.service.DriverService;
import com.carrental.system.login.service.EmployeeService;
import com.carrental.system.vehicle.entity.DispatchLog;
import com.carrental.system.vehicle.entity.DispatchStatus;
import com.carrental.system.vehicle.entity.Vehicle;
import com.carrental.system.vehicle.exception.CustomValidationException;
import com.carrental.system.vehicle.service.DispatchLogService;
import com.carrental.system.vehicle.service.VehicleService;

@RestController
@RequestMapping("/api/dispatchlog")
@CrossOrigin
public class DispatchLogController {

	private DispatchLogService dispatchLogService;
	private VehicleService vehicleService;
    private DriverService driverService;       
    private EmployeeService employeeService; 
	
	@Autowired
	public DispatchLogController(DispatchLogService dispatchLogService, VehicleService vehicleService, DriverService driverService, EmployeeService employeeService) {
		this.dispatchLogService = dispatchLogService;
		this.vehicleService = vehicleService;
		this.driverService = driverService;
		this.employeeService = employeeService;
	}
	
//	@Autowired
//	public DispatchLogController(VehicleService vehicleService) {
//		this.vehicleService = vehicleService;
//	}
	
//	================ 一、對外 API ================
	
	
//	==================== 二、CRUD ====================
	
//	查單筆
	@GetMapping("/{dispatchId}")
	public DispatchLog getDispatchLog(@PathVariable("dispatchId") Integer dispatchId) {
		DispatchLog dispatchLog = dispatchLogService.getDispatchLogById(dispatchId);
		return dispatchLog;
	}
	
//	查全部
	@GetMapping
	public List<DispatchLog> listDispatchLogs(){
		return dispatchLogService.getAllDispatchLogs();
	}
	
//	分頁查詢(並可依調度狀態) *Spring Boot頁數從0起算
	@GetMapping("/page")
	public ResponseEntity<Page<DispatchLog>> pageDispatchLogs(
			@RequestParam(defaultValue = "0") int page,           // 預設第0頁(也就是第1頁)
			@RequestParam(defaultValue = "10") int size,  		  // 預設每頁10筆
			@RequestParam(required = false) DispatchStatus status // 可選的狀態篩選
			){
		// 建立Pageable物件，並設定「依調度單ID升冪排序(依建立順序)」
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "dispatchId"));
		Page<DispatchLog> pageResult = dispatchLogService.getDispatchLogs(status, pageable);
		return ResponseEntity.ok(pageResult);
	}
	
//	by vehicleId查詢
	@GetMapping("/vehicle/{vehicleId}")
	public ResponseEntity<List<DispatchLog>> getDispatchLogsByVehicleId(@PathVariable Integer vehicleId){
		List<DispatchLog> logs = dispatchLogService.getDispatchByVehicleId(vehicleId);
		return ResponseEntity.ok(logs);
	}
	
//	by status查詢
//	@GetMapping("/counts")
//	public ResponseEntity<Map<String, Long>> getDispatchCounts(){
//		Map<String, Long> counts = new HashMap<String, Long>();
//		
//		counts.put("PENDING", dispatchLogRepository.countByStatus(DispatchStatus.PENDING));
//		counts.put("IN_PROCESS", dispatchLogRepository.countByStatus(DispatchStatus.IN_PROCESS));
//		counts.put("FINISHED", dispatchLogRepository.countByStatus(DispatchStatus.FINISHED));
//		counts.put("CANCEL", dispatchLogRepository.countByStatus(DispatchStatus.CANCEL));
//		
//		return ResponseEntity.ok(counts);
//	}
	
//	增
	@PostMapping
	public ResponseEntity<?> addDispatchLog(@RequestBody DispatchLog dispatchLog) {
		
		LocalDateTime startTime = dispatchLog.getScheduledStartTime(); //8
		LocalDateTime endTime = startTime.plusHours(VehicleService.DISPATCHED_FIXED_HOURS); //10
		
		// 呼叫VehicleService API2: 建單雙重驗證，若有錯會跳出詳細無法建單原因
		vehicleService.checkAvailableForBooking(dispatchLog.getVehicle().getVehicleId(), startTime, endTime);
		
		// 驗證通過後，存入資料庫
		DispatchLog savedDispatchLog = dispatchLogService.createDispatchLog(dispatchLog);
		return ResponseEntity.ok(savedDispatchLog);
	}
	
//	改
	@PutMapping("/{dispatchId}")
	public DispatchLog editDispatchLog(
			@PathVariable("dispatchId") Integer dispatchId,
			@RequestBody DispatchLog dispatchLog) {
		dispatchLog.setDispatchId(dispatchId);
		return dispatchLogService.updateDispatchLog(dispatchLog);
	}
	
//	刪
	@DeleteMapping("/{dispatchId}")
	public String removeDispatchLog(@PathVariable("dispatchId") Integer dispatchId) {
		DispatchLog dispatchLog = dispatchLogService.getDispatchLogById(dispatchId);
		if (dispatchLog != null) {
			dispatchLogService.deleteDispatchLog(dispatchId);
			return "刪除成功";
		}
		return "查無此ID調度紀錄，無法刪除";
	}

//	================ 三、本身狀態機＆特殊商業邏輯 ================
//	流派二：實用主義 REST-RPC 混合 (動作導向流)
//	當一個操作不單純只是 CRUD（增刪改查），而是會觸發一連串副作用（Side-effects）的「業務動作」時，在 URI 後面加上動詞是業界公認的實務做法。
//	像是按下「完成調度」，系統不只改了 status，還連帶更新了一張表（Vehicle）的 mileage 與 vehicle_status。
//	許多頂尖科技公司（如 Stripe, GitHub）遇到這類業務動作時，也是採用這種寫法。例如 Stripe 針對請款的操作就是 POST /v1/charges/{id}/capture。
//	優缺點：
//	意圖極度清晰，Service 層的方法職責單一（Single Responsibility），非常好維護。但通常會搭配 HTTP POST 方法（因為 PUT 的語意是「完整替換資源」，而 POST 常被作為執行自訂動作的萬用字元）。
	
//	開始調度：狀態轉換＋更新「調度」起始里程（調度狀態轉「執行中」，車輛狀態轉「調度中」）
	@PostMapping("/{dispatchId}/start")
	public ResponseEntity<?> startDispatch(@PathVariable("dispatchId") Integer dispatchId){
		dispatchLogService.startDispatch(dispatchId);
		return ResponseEntity.ok().body("調度已開始");
	}
//	完成調度：狀態轉換＋更新「調度」的狀態、結束里程、結束時間，及「車輛」的狀態、里程
	@PostMapping("/{dispatchId}/finish")
	public ResponseEntity<?> finishDispatch(
			@PathVariable("dispatchId") Integer dispatchId,
			@RequestParam("endMileage") Integer endMileage){
		dispatchLogService.finishDispatch(dispatchId, endMileage);
		return ResponseEntity.ok().body("調度已完成，里程與車輛狀態已更新");
	}
//	取消調度：狀態轉換＋更新「調度」的狀態，及「車輛」的狀態
	@PostMapping("/{dispatchId}/cancel")
	public ResponseEntity<?> cancelDispatch(@PathVariable("dispatchId") Integer dispatchId){
		dispatchLogService.cancelDispatch(dispatchId);
		return ResponseEntity.ok().body("調度單已取消");
	}
	
//	================ 四、私有輔助方法 ================
	
//	輔助方法1：拋出CustomValidationException
	private void throwCustomValidationException(String fieldName, String errorString) {
		Map<String, String> errors = new HashMap<String, String>();
		errors.put(fieldName, errorString);
		throw new CustomValidationException(errors);
	}
	
 // ================ 五、繞過權限的下拉選單專用 API ================
    
    // 取得所有司機
    @GetMapping("/drivers")
    public ResponseEntity<List<DriverResponseDTO>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getAllDrivers());
    }
    
    @GetMapping("/drivers/{driverId}")
    public ResponseEntity<DriverResponseDTO> getDriverById(@PathVariable Integer driverId) {
        Optional<DriverResponseDTO> driver = driverService.getDriverById(driverId);
        return driver.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
	
    // 取得所有員工 (填單人)
    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeResponseDTO>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }
    
    @GetMapping("/employees/{empId}")
    public ResponseEntity<DriverResponseDTO> getDriverByEmpId(@PathVariable Integer empId) {
        Optional<DriverResponseDTO> driver = driverService.getDriverByEmpId(empId);
        return driver.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
	
}
