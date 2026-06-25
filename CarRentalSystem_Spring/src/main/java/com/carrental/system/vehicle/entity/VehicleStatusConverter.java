package com.carrental.system.vehicle.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

//autoApply = true 讓 JPA 自動在所有用到 VehicleStatus 的地方套用此轉換器
@Converter(autoApply = true)
public class VehicleStatusConverter implements AttributeConverter<VehicleStatus	, String> {

	// 寫入資料庫時：將 Enum 轉成 String (如: VehicleStatus.CLEANING -> "CLEANING")
	@Override
	public String convertToDatabaseColumn(VehicleStatus attribute) {
		if (attribute == null) {
			return null;
		}
		return attribute.getDbCode();
	}

	// 從資料庫讀出時：將 String 轉回 Enum (如: "CLEANING" -> VehicleStatus.CLEANING)
	@Override
	public VehicleStatus convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.trim().isEmpty()) {
			return null;
		}
		try {
			return VehicleStatus.fromDbCode(dbData.trim());
		} catch (Exception e) {
			System.out.println("轉換失敗！資料庫裡出現未知的狀態字串：[" + dbData + "]");
			throw new IllegalArgumentException("無法轉換的狀態" + dbData, e);
		}
	}
}
