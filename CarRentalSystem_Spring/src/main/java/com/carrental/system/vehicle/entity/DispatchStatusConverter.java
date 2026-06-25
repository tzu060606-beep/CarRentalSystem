package com.carrental.system.vehicle.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DispatchStatusConverter implements AttributeConverter<DispatchStatus, String>{

	@Override
	// 寫入資料庫時：將 Enum 轉成 String (如: DispatchStatus.PENDING -> "PENDING")
	public String convertToDatabaseColumn(DispatchStatus attribute) {
		if (attribute == null) {
			return null;
		}
		return attribute.getDbCode();
	}

	@Override
	// 從資料庫讀出時：將 String 轉回 Enum (如: "PENDING" -> DispatchStatus.PENDING)
	public DispatchStatus convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.trim().isEmpty()) {
			return null;
		}
		try {
			return DispatchStatus.fromDbCode(dbData.trim());
		} catch (Exception e) {
			System.out.println("轉換失敗！資料庫裡出現未知的狀態字串：[" + dbData + "]");
			throw new IllegalArgumentException("無法轉換的狀態" + dbData, e);
		}
	}
}
