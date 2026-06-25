package com.carrental.system.vehicle.exception;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.carrental.system.vehicle") //加上basePackages限定作用在vehicle package內
public class GlobalExceptionHandler {
	
	//TODO:補筆記
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	
	
//	================================
//			400: Bad Request
//	================================
//	攔截IllegalArgumentException
	@ExceptionHandler(IllegalArgumentException.class   )
	public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex){
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("status", 400);
		
		// 抓取service錯誤訊息
		response.put("message", ex.getMessage());
		// 為配合前端資料結構，這裡塞一個空的fieldErrors，前端才不會undefined報錯
		response.put("fieldErrors", new HashMap<>());
		return ResponseEntity.badRequest().body(response);
	}
	
//	攔截自訂驗證錯誤
	@ExceptionHandler(CustomValidationException.class)
	public ResponseEntity<Map<String, Object>> handleCustomValidationExceptions(CustomValidationException ex){
		
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("status", 400);
		response.put("message", ex.getMessage()); //「資料驗證失敗」
		
		// 將手動收集的 fieldErrors 塞進JSON
		response.put("fieldErrors", ex.getFieldErrors());
		
		return ResponseEntity.badRequest().body(response);
	}
	
//	攔截RuntimeException
	@ExceptionHandler(RuntimeException.class   )
	public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex){
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("status", 500);
		
		// 抓取service錯誤訊息
		response.put("message", ex.getMessage());
		// 為配合前端資料結構，這裡塞一個空的fieldErrors，前端才不會undefined報錯
		response.put("fieldErrors", new HashMap<>());
		return ResponseEntity.badRequest().body(response);
	}
}
