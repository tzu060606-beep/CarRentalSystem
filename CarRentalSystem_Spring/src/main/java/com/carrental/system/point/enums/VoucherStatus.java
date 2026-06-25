package com.carrental.system.point.enums;

public enum VoucherStatus {
	
	UNUSED("未使用"),   // 兌換成功，等待使用
	USED("已使用"),      //客戶已使用票券
	EXPIRED("已過期");   //票券超過使用期限
	
	// 【1】宣告欄位，用來存中文名稱
		private final String displayName;

		// 【2】建構子，當 ACTIVE("待使用") 被建立時java會呼叫這個建構子，把 "待使用" 傳進來
		VoucherStatus(String displayName) {
			this.displayName = displayName; // 把 "待使用" 存進欄位裡
		}

		// 【3】getter，讓外部可以取得中文名稱
		public String getDisplayName() {
			return displayName;// 回傳存起來的 "待使用"
		}

}
