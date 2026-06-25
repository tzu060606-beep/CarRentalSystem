package com.carrental.system.usedCar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import com.carrental.system.login.entity.CustomerBean;
import com.carrental.system.login.repository.CustomerRepository;
import com.carrental.system.usedCar.DTO.ViewingAppointmentDto;
import com.carrental.system.usedCar.entity.UsedCarBean;
import com.carrental.system.usedCar.entity.ViewingAppointmentBean;
import com.carrental.system.usedCar.repository.UsedCarRepository;
import com.carrental.system.vehicle.entity.Location;
import com.carrental.system.vehicle.repository.LocationRepository;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class ViewingAppointmentMapper {

	@Autowired
	protected LocationRepository locationRepo;
	
	@Autowired 
	protected UsedCarRepository usedCarRepo; 
    
	@Autowired 
	protected CustomerRepository customerRepo; 
	// 注入 Repository

	// 1. DTO 轉 Entity：手動處理 location
    @Mapping(target = "location", source = "locationId", qualifiedByName = "idToLocation")
    @Mapping(target = "usedCar", source = "usedCarId", qualifiedByName = "idToUsedCar")
    @Mapping(target = "customer", source = "custId", qualifiedByName = "idToCustomer")
    public abstract ViewingAppointmentBean toEntity(ViewingAppointmentDto dto);

	// 2. Entity 轉 DTO：手動處理 location_id
    @Mapping(target = "locationId", source = "location.locationId")
    @Mapping(target = "usedCarId", source = "usedCar.usedCarId")
    @Mapping(target = "custId", source = "customer.custId")
    @Mapping(target = "locationName", source = "location.locationName")
    public abstract ViewingAppointmentDto toDto(ViewingAppointmentBean bean);

    @Mapping(target = "location", source = "locationId", qualifiedByName = "idToLocation")
    @Mapping(target = "usedCar", source = "usedCarId", qualifiedByName = "idToUsedCar")
    @Mapping(target = "customer", source = "custId", qualifiedByName = "idToCustomer")
    public abstract void updateViewingAppointmentFromDto(ViewingAppointmentDto dto, @MappingTarget ViewingAppointmentBean entity);
    
 // --- 以下就是你的工具方法 (Helper Methods) ---

    @Named("idToLocation")
    protected Location idToLocation(Integer id) {
        return id == null ? null : locationRepo.findById(id).orElse(null);
    }

    @Named("idToUsedCar")
    protected UsedCarBean idToUsedCar(Integer id) {
    	System.out.println(">>> [DEBUG] Mapper 正在嘗試轉換 ID: " + id);
    	if (id == null) {
            return null;
        }
        // 正確的寫法：先 findById，然後用 orElseThrow 處理 Optional
        return usedCarRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到編號為 " + id + " 的二手車資料"));
    }

    @Named("idToCustomer")
    protected CustomerBean idToCustomer(Integer id) {
        return id == null ? null : customerRepo.findById(id).orElse(null);
    }
}
