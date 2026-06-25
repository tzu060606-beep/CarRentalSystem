package com.carrental.system.usedCar.apiAndException;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.carrental.system.usedcar") //加上basePackages限定作用在usedcar package內
public class GlobalExceptionHandlerUsedCar {
	
	//TODO:補筆記
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandlerUsedCar.class);
	
	
	// 1. 攔截自訂驗證錯誤 (最常發生)
    @ExceptionHandler(UsedCarException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleCustomValidationExceptions(UsedCarException ex) {
        log.warn("驗證失敗: {}", ex.getMessage());
        // 使用 ApiResponse 統一格式
        ApiResponse<Map<String, String>> response = ApiResponse.error(400, ex.getMessage());
        response.setData(ex.getFieldErrors()); 
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    // 2. 攔截非法參數異常
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("非法參數錯誤: ", ex);
        return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
    }
    
    // 3. 攔截所有其他 RuntimeException (例如 NullPointerException)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
        log.error("執行時錯誤: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "伺服器內部錯誤：" + ex.getMessage()));
    }

    // 4. 最後防線：攔截所有剩餘的 Exception (例如 IO 錯誤)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(Exception ex) {
        log.error("系統發生未預期錯誤: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "系統發生未預期錯誤"));
    }
}
