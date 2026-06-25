package com.carrental.system.usedCar.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import com.carrental.system.usedCar.DTO.UsedCarDto;
import com.carrental.system.usedCar.entity.UsedCarBean;
import com.carrental.system.vehicle.entity.Vehicle;
import com.carrental.system.vehicle.repository.VehicleRepository;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class UsedCarMapper {

	@Autowired
    protected VehicleRepository vehicleRepo; // 注入 Vehicle 的倉庫

    // 1. 將 DTO 轉為 Entity (用於新增)
    @Mapping(target = "vehicle", source = "vehicleId", qualifiedByName = "idToVehicle")
    public abstract UsedCarBean toEntity(UsedCarDto dto);

    // 2. 將 Entity 轉為 DTO (用於回傳給前端)
    @Mapping(target = "vehicleId", source = "vehicle.vehicleId") // 假設 Vehicle 裡的 PK 叫 vehicleId
    public abstract UsedCarDto toDto(UsedCarBean bean);

    // 3. 更新邏輯
    @Mapping(target = "vehicle", source = "vehicleId", qualifiedByName = "idToVehicle")
    public abstract void updateUsedCarFromDto(UsedCarDto dto, @MappingTarget UsedCarBean entity);

    // --- 工具方法：ID 轉物件 ---

    @Named("idToVehicle")
    protected Vehicle idToVehicle(Integer id) {
        if (id == null) return null;
        return vehicleRepo.findById(id).orElse(null);
    }
}