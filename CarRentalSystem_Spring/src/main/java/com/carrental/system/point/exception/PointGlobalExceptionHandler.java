package com.carrental.system.point.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.carrental.system.point.dto.ApiResponse;

//用basePackages限定只攔截point package發出的的例外
@RestControllerAdvice(basePackages = "com.carrental.system.point")
// （處理全域攔截 + JSON回傳）結合@ControllerAdvice和@ResponseBody
public class PointGlobalExceptionHandler {

	// [重構]- 需要更具體的ExceptionHandler，否則範圍太大沒有意義，全部都回傳一樣的狀態碼
	// 切細意圖:前端可根據狀態碼判斷回饋，相較接收純文字判斷更穩健
	// 建立自訂 Exception 類別（繼承 RuntimeException）
	// 在 GlobalExceptionHandler 新增對應的 handler

	// 【A】攔截ResourceNotFoundException(包括找不到商品、找不到客戶、找不到訂單)
	// 狀態碼 404
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiResponse> handleResourceNotFoundException(
			ResourceNotFoundException resourceNotFoundException) {
		ApiResponse apiResponse = new ApiResponse(false, resourceNotFoundException.getMessage(), null);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
	}

	// 【B】攔截BusinessRuleException(包括點數不足、庫存不足、商品下架中)
	// 狀態碼 422
	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<ApiResponse> handleBusinessRuleException(BusinessRuleException businessRuleException) {
		ApiResponse apiResponse = new ApiResponse(false, businessRuleException.getMessage(), null);
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(apiResponse);
	}

	// 【打底用】 攔截 RuntimeException，回傳 ApiResponse(false, ex.getMessage(), null)，HTTP
	// 狀態碼
	// 狀態碼 400
	@ExceptionHandler(RuntimeException.class) // （要指定處理哪種例外）
	public ResponseEntity<ApiResponse> handleRunTimeException(RuntimeException runtimeException) {

		ApiResponse apiResponse = new ApiResponse(false, runtimeException.getMessage(), null);
		// apiResponse有設全參，可簡化寫法
		// apiResponse.setSuccess(false);
		// apiResponse.setMessage(runtimeException.getMessage());
		// apiResponse.setData(null);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
	}

	// 【打底用】 攔截 Exception，回傳 ApiResponse(false, "系統錯誤，請稍後再試", null)，HTTP 狀態碼 500
	// @ExceptionHandler 裡換成 Exception.class
	// 狀態碼換成 HttpStatus.INTERNAL_SERVER_ERROR
	// 訊息固定寫死，不從 ex.getMessage() 取
	@ExceptionHandler(Exception.class) // （指定處理Exception例外）
	public ResponseEntity<ApiResponse> handleException(Exception exception) {
		ApiResponse apiResponse = new ApiResponse(false, "系統錯誤，請稍後再試", null);
//		apiResponse.setSuccess(false);
//		apiResponse.setMessage("系統錯誤，請稍後再試");
//		apiResponse.setData(null);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
	}
	
	// 攔截 @Valid 驗證失敗
	// 狀態碼 400
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse> handleValidationException(
	        MethodArgumentNotValidException ex) {
	    // 取第一個錯誤訊息回傳
	    String message = ex.getBindingResult()
	        .getFieldErrors()
	        .stream()
	        .map(e -> e.getField() + "：" + e.getDefaultMessage())
	        .findFirst()
	        .orElse("格式驗證失敗");
	    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	        .body(new ApiResponse(false, message, null));
	}
	
}
