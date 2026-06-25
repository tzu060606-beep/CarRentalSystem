package com.carrental.system.point.enums;

public enum ChangeType {
	
	RENTAL("租車累點", "+"), 
	TRANSFER("專車累點", "+"), 
	BIRTHDAY("生日贈點", "+"), 
	WELCOME_BONUS("新會員註冊贈點", "+"), 
	EXPIRED("點數過期", "-"), 
	REDEMPTION("兌換消耗", "-"),
	FIRST_RENTAL("首租獎勵","+");

	// 【1】宣告欄位，用來存中文名稱、正負符號
	private final String displayName;
	private final String sign;

	// 【2】建構子，當RENTAL("租車獲得") 被建立時java會呼叫這個建構子，把 "租車獲得" 和正負號傳進來
	ChangeType(String displayName, String sign) {
		this.displayName = displayName;
		this.sign = sign;
	}

	// 【3】getter，讓外部可以取得中文名稱、正負號
	public String getDisplayName() {
		return displayName;
	}

	public String getSign() {
		return sign;
	}

}
