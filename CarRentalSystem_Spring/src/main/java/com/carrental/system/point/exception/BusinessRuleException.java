package com.carrental.system.point.exception;

//業務規則不符」的情境 → Handler 回 422
public class BusinessRuleException extends RuntimeException {

	// 建構子：當別人 throw new BusinessRuleException("點數不足") 時會執行這裡
	public BusinessRuleException(String message) {

		// 呼叫父類別 RuntimeException 的建構子
		// 把 message 往上傳，最終存進 Throwable 的 detailMessage 欄位
		// 之後 Handler 才能用 ex.getMessage() 拿到 "商品不存在"
		super(message);

	}

}
