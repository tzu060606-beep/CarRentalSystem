package com.carrental.system.usedCar.apiAndException;

import java.util.HashMap;
import java.util.Map;

// 繼承 RuntimeException，讓他可以被自由拋出
public class UsedCarException extends RuntimeException{

	// 用來裝載「哪個欄位」對應「什麼錯誤訊息」
	private final Map<String, String> fieldErrors;
	
	public UsedCarException(Map<String, String> fieldErrors) {
		super("資料驗證失敗"); 
		this.fieldErrors = fieldErrors;
		
	}
	// 讓全域處理器可以拿回這些錯誤
	public Map<String, String> getFieldErrors(){
		return fieldErrors;
	}
	
}
