package com.carrental.system.rentalorder.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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

import com.carrental.system.rentalorder.dto.AdminLongTermReqDto;
import com.carrental.system.rentalorder.dto.AdminOrderReqDto;
import com.carrental.system.rentalorder.dto.AdminOrderUpdateReqDto;
import com.carrental.system.rentalorder.dto.AdminShortTermReqDto;
import com.carrental.system.rentalorder.dto.MultiInsertRequest;
import com.carrental.system.rentalorder.entity.LongTermDetailBean;
import com.carrental.system.rentalorder.entity.RentalOrderBean;
import com.carrental.system.rentalorder.entity.ShortTermDetailBean;
import com.carrental.system.rentalorder.enums.FuelLevel;
import com.carrental.system.rentalorder.enums.OrderType;
import com.carrental.system.rentalorder.enums.PaymentMethod;
import com.carrental.system.rentalorder.service.OrderService;
import com.carrental.system.vehicle.entity.Vehicle;
import com.carrental.system.vehicle.exception.CustomValidationException;
import com.carrental.system.vehicle.service.VehicleService;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/admin/rentalorders")

@CrossOrigin(origins = "*") // 開發階段可以先設定 *，上線時再限制網域

public class RentalOrderController {
	
	
	@Autowired
	private OrderService oService;

	@Autowired
	private VehicleService vehicleService;
//------------單筆查詢--------------------------
	// 【執行順序 1】：由 Spring 框架攔截 URL 請求，將網址路徑中的變數提取出來並進入此方法
	@GetMapping(path = "/query/{query}")
	public ResponseEntity<?> orderQueryAction(@PathVariable("query") Integer orderId) {
//		- ResponseEntity: [回應實體] Spring 提供的類別，用來封裝「整份」HTTP 回應（包含狀態碼、標頭、內容物）。
		
		try {

			RentalOrderBean order = oService.getOrder(orderId);
			return ResponseEntity.ok(order);
			
		} catch (NoSuchElementException e) {

			Map<String, String> error = new HashMap<>();
            error.put("error", "查無此訂單，請確認訂單編號：" + orderId);
            
            // 🌟 4. 回傳 404 Not Found + 錯誤訊息
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

			// 之前不用try-catch寫
			// return ResponseEntity.notFound().build();
			//以下為解釋
			
			/* 
	           - ResponseEntity.notFound(): [靜態方法] 產生一個 HTTP 404 狀態碼。
	           - .build(): [建立] 因為 404 通常不需要帶內容物（Body），所以呼叫 build 直接把這份空的回應蓋好並送出。
	           //它會把剛才設定好的狀態碼（404）打包成一個真正的ResponseEntity 物件
	           //notFound()：因為 404 狀態通常不帶資料，但有時候開發者想加一些特殊的 HTTP Header（標頭）。
	           //所以它先給你一個半成品，讓你有機會加東西，最後才叫你手動點選「確認送出 (.build())」。
	           
	        */


		}

		
	}
	
//------------查詢全部--------------------------
@GetMapping("/all")
public ResponseEntity<List<RentalOrderBean>> getAllOrders() {
	   List<RentalOrderBean> list = oService.findAllOrder(); // 請確保 Service 有這個方法
	    return ResponseEntity.ok(list);
	}


//------------新增--------------------------
	@PostMapping("/new")
	public ResponseEntity<?> insertAction(@RequestBody AdminOrderReqDto request) {
		try {

		RentalOrderBean newOrder = oService.insert(request);
		return ResponseEntity.ok(newOrder);	
			
		}catch (CustomValidationException e) {
        Map<String, String> error = new HashMap<>();
        
        // 從 fieldErrors 保險箱裡，把裝著 "vehicle" 或 "time" 的後面訊息挖出來
        Map<String, String> fieldErrors = e.getFieldErrors();
        String realMessage = "資料驗證失敗"; // 預設值
        
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            // 抓一條真實的錯誤訊息出來顯示 (通常只會有一條)
            realMessage = fieldErrors.values().iterator().next(); 
        }
        
        error.put("error", "錯誤：" + realMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
		
	
		}catch (NoSuchElementException e) {

			Map<String, String> error = new HashMap<>();
			error.put("error","找不到資料:"+e.getMessage());

			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

			
		}catch(IllegalArgumentException e){

			Map<String, String> error = new HashMap<>();
			error.put("error","錯誤："+e.getMessage());

			// 回傳 400 Bad Request
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

		}catch(RuntimeException e){

			Map<String, String> error = new HashMap<>();
			error.put("error","錯誤："+e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

		}

		

	}

	
//------------刪除--------------------------
	
	@DeleteMapping("/{orderId}")
	public ResponseEntity<Map<String, String>> orderDeleteAction (@PathVariable("orderId") Integer orderId){
		
		try {

			oService.delete(orderId);
		
		// 回傳 200 OK 並附帶 JSON 訊息
	    Map<String, String> response = new HashMap<>();
	    response.put("message:", "訂單已成功刪除 (ID: " + orderId + ")");
	    
	    return ResponseEntity.ok(response);
		/*ResponseEntity (回應實體)： 這就是郵局規定的「標準包裹紙箱」。這個紙箱外面會貼著「狀態碼（200 成功、404 找不到）」
		 *.ok()：在紙箱外面蓋上一個大大的印章：HTTP Status 200 (成功)。這個也會傳出去，前端很依賴此作判斷
		 *(order)：把你的商品放進紙箱的「內容物 (Body)」裡面。
		 *
		 */
			
		} catch (RuntimeException e) {
			Map<String, String> error = new HashMap<>();
        	error.put("error", "刪除失敗：" + e.getMessage());
        
        	// 找不到東西，回傳 404 Not Found 給前端
        	return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
		}

		 
	}


//------------舊更新方法-----------------------
	// @PutMapping("/{orderId}")
	// public RentalOrderBean updateAction(
	// 		@PathVariable("orderId") Integer orderId, // 把網址的 ID 抓下來
	//         @RequestBody RentalOrderBean updateBean) {
	// 	updateBean.setOrderId(orderId); // 強制把 JSON 裡的 ID 蓋掉，以網址為準！
	// 	return oService.update1(updateBean);
		
	// }

//------------新更新方法-----------------------

@PutMapping("/{orderId}")
public ResponseEntity<?> UpdateOrder(
	@PathVariable("orderId")Integer orderId,
	@RequestBody AdminOrderUpdateReqDto orderUpdateDto){

		try {

			RentalOrderBean adminUpdateOrder = oService.adminUpdateOrder(orderId,orderUpdateDto);
			return ResponseEntity.ok(adminUpdateOrder);
			
		} catch (EntityNotFoundException e) {

			Map<String, String> error = new HashMap<>();
        	error.put("error", "資料庫關聯實體遺失：" + e.getMessage());
			
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
			
		}catch (RuntimeException e) {

			Map<String, String> error = new HashMap<>();
        	error.put("error", "錯誤：" + e.getMessage());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
		
		
		}






}


// ------------------
// @GetMapping("/available-vehicles")
// public ResponseEntity<List<Vehicle>> getAvailableVehicles(
//         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime pickupTime,
//         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime returnTime) {
    
//     // 呼叫 F6 提供的第一道防線方法，取得空車清單
//     List<Vehicle> availableCars = vehicleService.getAvailableVehicles(pickupTime, returnTime);
    
//     return ResponseEntity.ok(availableCars);
// }



//----------------要寫及時計算的api-----------



// -------------狀態機專用 API (取車、還車、結案、取消)-----------------

// 1. 取車 API
@PutMapping("/{orderId}/pickup")
public ResponseEntity<?> processPickup(
        @PathVariable("orderId") Integer orderId,
        @RequestBody Map<String, Object> payload) { // 🌟 新增：接收前端傳來的 JSON 表單
    try {
        // 從 payload 解析出里程與備註
        Integer mileage = null;
        if(payload.get("startMileage") != null && !payload.get("startMileage").toString().isEmpty()){
            mileage = Integer.valueOf(payload.get("startMileage").toString());
        }
        String note = (String) payload.get("note");

        // 呼叫剛剛改好的 Service 方法
        RentalOrderBean updatedOrder = oService.pickupOrder(orderId, mileage, note);
        return ResponseEntity.ok(updatedOrder);

		}catch (CustomValidationException e) {
        Map<String, String> error = new HashMap<>();
        
        // 從 fieldErrors 保險箱裡，把裝著 "vehicle" 或 "time" 的後面訊息挖出來
        Map<String, String> fieldErrors = e.getFieldErrors();
        String realMessage = "資料驗證失敗"; // 預設值
        
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            // 抓一條真實的錯誤訊息出來顯示 (通常只會有一條)
            realMessage = fieldErrors.values().iterator().next(); 
        }
        
        error.put("error", "錯誤：" + realMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

    } catch (Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "錯誤：" + e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}


// 2. 還車 API
@PutMapping("/{orderId}/return")
public ResponseEntity<?> processReturn(
        @PathVariable("orderId") Integer orderId,
        @RequestBody Map<String, Object> payload) { 
    try {
        Integer mileage = null;
        if(payload.get("endMileage") != null && !payload.get("endMileage").toString().isEmpty()){
            mileage = Integer.valueOf(payload.get("endMileage").toString());
        }
        
        // 新增：把前端傳來的字串，轉成你的 FuelLevel 枚舉！
        String fuelStr = (String) payload.get("fuel");
        FuelLevel fuelLevel = null; 
        if (fuelStr != null && !fuelStr.trim().isEmpty()) {
            fuelLevel = FuelLevel.valueOf(fuelStr); // 字串轉枚舉的核心武器
        }

        String note = (String) payload.get("note");

        //  這裡傳入的第三個參數，已經變成安全的 FuelLevel 枚舉了
        RentalOrderBean updatedOrder = oService.returnOrder(orderId, mileage, fuelLevel, note);
        return ResponseEntity.ok(updatedOrder);

	}catch (CustomValidationException e) {
        Map<String, String> error = new HashMap<>();
        
        // 從 fieldErrors 保險箱裡，把裝著 "vehicle" 或 "time" 的後面訊息挖出來
        Map<String, String> fieldErrors = e.getFieldErrors();
        String realMessage = "資料驗證失敗"; // 預設值
        
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            // 抓一條真實的錯誤訊息出來顯示 (通常只會有一條)
            realMessage = fieldErrors.values().iterator().next(); 
        }
        
        error.put("error", "錯誤：" + realMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

    } catch (Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "錯誤：" + e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}

// 3. 結案 API
@PutMapping("/{orderId}/close")
public ResponseEntity<?> processClose(
	@PathVariable("orderId") Integer orderId,
	@RequestBody Map<String, Object> payload) {
    try {
        // 結案時由前端帶入尾款付款方式，後端統一轉成 PaymentMethod enum
        Object balancePayMethodValue = payload == null ? null : payload.get("balancePayMethod");
        PaymentMethod balancePayMethod = PaymentMethod.fromValue(
            balancePayMethodValue == null ? null : balancePayMethodValue.toString()
        );

        if (balancePayMethod == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "錯誤：請選擇尾款付款方式");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        RentalOrderBean updatedOrder = oService.closeOrder(orderId, balancePayMethod);
        return ResponseEntity.ok(updatedOrder);
	
	}catch (CustomValidationException e) {
        Map<String, String> error = new HashMap<>();
        
        // 從 fieldErrors 保險箱裡，把裝著 "vehicle" 或 "time" 的後面訊息挖出來
        Map<String, String> fieldErrors = e.getFieldErrors();
        String realMessage = "資料驗證失敗"; // 預設值
        
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            // 抓一條真實的錯誤訊息出來顯示 (通常只會有一條)
            realMessage = fieldErrors.values().iterator().next(); 
        }
        
        error.put("error", "錯誤：" + realMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    } catch (Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "錯誤：" + e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
// 4.取消API
@PutMapping("/{orderId}/cancel")
public ResponseEntity<?>processCancel(@PathVariable("orderId") Integer orderId){
	try {

		RentalOrderBean updatedOrder = oService.cancelOrder(orderId);
		return ResponseEntity.ok(updatedOrder);

	}catch (CustomValidationException e) {
        Map<String, String> error = new HashMap<>();
        
        // 從 fieldErrors 保險箱裡，把裝著 "vehicle" 或 "time" 的後面訊息挖出來
        Map<String, String> fieldErrors = e.getFieldErrors();
        String realMessage = "資料驗證失敗"; // 預設值
        
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            // 抓一條真實的錯誤訊息出來顯示 (通常只會有一條)
            realMessage = fieldErrors.values().iterator().next(); 
        }
        
        error.put("error", "錯誤：" + realMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
		
	} catch (Exception e) {
		
		Map<String, String> error = new HashMap<>();
        error.put("error", "錯誤：" + e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}
}

// 5.完成退款API
@PutMapping("/{orderId}/complete-refund")
public ResponseEntity<?> processCompleteRefund(@PathVariable("orderId") Integer orderId){
	try {

		RentalOrderBean updatedOrder = oService.completeRefund(orderId);
		return ResponseEntity.ok(updatedOrder);

	} catch (Exception e) {
		Map<String, String> error = new HashMap<>();
        error.put("error", "錯誤：" + e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}
}
// -------------多條件搜尋-----------------
 @PostMapping("/search")
    public ResponseEntity<?> searchOrders(@RequestBody Map<String, String> searchParams) {
        
		try {

			List<RentalOrderBean> list = oService.filterOrders(searchParams);
        return ResponseEntity.ok(list);
			
		} catch (EntityNotFoundException e) {

			Map<String, String> error = new HashMap<>();
			error.put("error","錯誤："+e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
			
		}catch (RuntimeException e) {
			
			Map<String, String> error = new HashMap<>();
			error.put("error","錯誤："+e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
		}
		
		
    }



}
