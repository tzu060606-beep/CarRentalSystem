package com.carrental.system.usedCar.EcPay;

public class OrderRequest {
    private String category;  // 組員填："USED_CAR" 或 "PARTS"
    private Long id;          // 組員填：該表的主鍵 ID
	private String tradeDesc; // 選填：訂單備註
	private String customItemName; // 新增這行

	public String getCustomItemName() {
		return customItemName;
	}

	public void setCustomItemName(String customItemName) {
		this.customItemName = customItemName;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTradeDesc() {
		return tradeDesc;
	}

	public void setTradeDesc(String tradeDesc) {
		this.tradeDesc = tradeDesc;
	}

}
