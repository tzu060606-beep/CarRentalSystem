package com.carrental.system.point.exception;

//「找不到資源」的情境 → Handler 回 404
public class ResourceNotFoundException extends RuntimeException {

	// 建構子
	// 先把RuntimeException初始化
	// 當別人 throw new ResourceNotFoundException("商品不存在") 時會執行這裡
	public ResourceNotFoundException(String message) {

		// 呼叫父類別 RuntimeException 的建構子
		// 把 message 往上傳，最終存進 Throwable 的 detailMessage 欄位
		// 之後 Handler 才能用 ex.getMessage() 拿到 "商品不存在"
		super(message);
	}

}
