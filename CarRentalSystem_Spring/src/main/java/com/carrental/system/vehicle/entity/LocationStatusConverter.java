package com.carrental.system.vehicle.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LocationStatusConverter implements AttributeConverter<LocationStatus, String>{

	@Override
	public String convertToDatabaseColumn(LocationStatus attribute) {
		if (attribute == null) {
			return null;
		}
		return attribute.getDbCode();
	}

	@Override
	public LocationStatus convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.trim().isEmpty()) {
			return null;
		}
		return LocationStatus.fromDbCode(dbData);
	}
}
