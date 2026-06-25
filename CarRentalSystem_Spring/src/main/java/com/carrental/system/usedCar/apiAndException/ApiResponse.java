package com.carrental.system.usedCar.apiAndException;

public class ApiResponse<T> {

	private Integer code; // 狀態碼 (例如：200代表成功，400代表前端傳錯，500代表後端炸了)
	private String message; // 訊息說明 (例如："查詢成功" 或 "找不到該車輛")
	private T data; // 實際的資料 (使用泛型 T，代表裡面可以裝單一車輛物件，也可以裝車輛 List)

	// --- 建構子 ---
	public ApiResponse(Integer code, String message, T data) {
		this.code = code;
		this.message = message;
		this.data = data;
	}

	// --- 靜態輔助方法 (為了讓你能在 Controller 快速呼叫) ---

	// 成功時呼叫此方法 (有回傳資料)
	public static <T> ApiResponse<T> success(String message, T data) {
		return new ApiResponse<>(200, message, data);
	}

	// 成功時呼叫此方法 (不需回傳資料，例如刪除成功)
	public static <T> ApiResponse<T> success(String message) {
		return new ApiResponse<>(200, message, null);
	}

	// 失敗時呼叫此方法
	public static <T> ApiResponse<T> error(Integer code, String message) {
		return new ApiResponse<>(code, message, null);
	}

	// --- Getter & Setter (一定要有，否則 Spring Boot 轉 JSON 會失敗) ---
	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}
}
