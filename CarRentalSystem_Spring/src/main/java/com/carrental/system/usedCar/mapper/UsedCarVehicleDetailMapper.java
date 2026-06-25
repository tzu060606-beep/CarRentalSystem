package com.carrental.system.usedCar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import com.carrental.system.usedCar.DTO.UsedCarDto;
import com.carrental.system.usedCar.DTO.UsedCarVehicleDetailDto;
import com.carrental.system.usedCar.entity.UsedCarBean;
import com.carrental.system.vehicle.entity.Vehicle;
import com.carrental.system.vehicle.repository.VehicleRepository;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class UsedCarVehicleDetailMapper {

	// 只有當你需要「根據 ID 找物件」存檔時才需要注入
	@Autowired
	protected VehicleRepository vehicleRepo;

	@Mapping(target = "usedCarId", source = "usedCarId")       // 二手車編號
	@Mapping(target = "askingPrice", source = "askingPrice")   // 價格
	@Mapping(target = "conditionDesc", source = "conditionDesc") // 車況
	@Mapping(target = "listDate", source = "listDate")         // 上架日期
	@Mapping(target = "usedCarStatus", source = "status")             // 二手車出售狀態 (注意不要跟車輛狀態搞混)
	@Mapping(target = "vehicleId", source = "vehicle.vehicleId")
	@Mapping(target = "plateNo", source = "vehicle.plateNo")
	@Mapping(target = "status", source = "vehicle.status")
	@Mapping(target = "color", source = "vehicle.color")
	@Mapping(target = "manufactureDate", source = "vehicle.manufactureDate")
	@Mapping(target = "issuedDate", source = "vehicle.issuedDate")
	@Mapping(target = "inspectionDate", source = "vehicle.inspectionDate")
	@Mapping(target = "mileage", source = "vehicle.mileage")
	@Mapping(target = "nextMaintainenceMileage", source = "vehicle.nextMaintainenceMileage")
	@Mapping(target = "description", source = "vehicle.description")
	@Mapping(target = "createdTime", source = "vehicle.createdTime")
	@Mapping(target = "modelId", source = "vehicle.carModel.modelId")
	@Mapping(target = "brand", source = "vehicle.carModel.brand")
	@Mapping(target = "modelName", source = "vehicle.carModel.modelName")
	@Mapping(target = "displacement", source = "vehicle.carModel.displacement")
	@Mapping(target = "turningRadius", source = "vehicle.carModel.turningRadius")
	@Mapping(target = "vehicleType", source = "vehicle.carModel.vehicleType")
	@Mapping(target = "seats", source = "vehicle.carModel.seats")
	@Mapping(target = "luggageCapacity", source = "vehicle.carModel.luggageCapacity")
	@Mapping(target = "fuelType", source = "vehicle.carModel.fuelType")
	@Mapping(target = "transmission", source = "vehicle.carModel.transmission")
	@Mapping(target = "wheelchairAvailable", source = "vehicle.carModel.wheelchairAvailable")
	@Mapping(target = "vehicleImgUrl", source = "vehicle.carModel.vehicleImgUrl")
	@Mapping(target = "locationId", source = "vehicle.location.locationId")
	@Mapping(target = "locationName", source = "vehicle.location.locationName")
	@Mapping(target = "address", source = "vehicle.location.address")
	@Mapping(target = "phone", source = "vehicle.location.phone")
	@Mapping(target = "parkingCapacity", source = "vehicle.location.parkingCapacity")
	@Mapping(target = "locationStatus", source = "vehicle.location.locationStatus")
	public abstract UsedCarVehicleDetailDto toVehicleDetailDto(UsedCarBean bean);

	// --- 寫入 (toEntity)：只需要直接關聯的 Repo ---
	@Mapping(target = "vehicle", source = "vehicleId", qualifiedByName = "idToVehicle")
	public abstract UsedCarBean toEntity(UsedCarDto dto);
	
	@Named("idToVehicle")
    protected Vehicle idToVehicle(Integer id) {
        return id == null ? null : vehicleRepo.findById(id).orElse(null);
    }

	
	protected java.sql.Timestamp map(java.time.LocalDateTime value) {
	    return value == null ? null : java.sql.Timestamp.valueOf(value);
	}
}
