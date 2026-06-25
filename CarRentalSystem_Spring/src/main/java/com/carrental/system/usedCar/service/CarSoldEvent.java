package com.carrental.system.usedCar.service;

public class CarSoldEvent {

	/**
	 * 💡 步驟 1：定義車輛成交事件 這是一個純 Java 類別 (POJO)，用來封裝成交車輛的關鍵資訊
	 */
	private final Integer usedCarId;

	public CarSoldEvent(Integer usedCarId) {
		this.usedCarId = usedCarId;
	}

	public Integer getUsedCarId() {
		return usedCarId;
	}
}
