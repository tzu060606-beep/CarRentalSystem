package com.carrental.system.usedCar.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import com.carrental.system.login.entity.CustomerBean;
import com.carrental.system.login.entity.EmployeeBean;
import com.carrental.system.login.repository.CustomerRepository;
import com.carrental.system.login.repository.EmployeeRepository;
import com.carrental.system.usedCar.DTO.SalesRecordDto;
import com.carrental.system.usedCar.entity.SalesRecordBean;
import com.carrental.system.usedCar.entity.UsedCarBean;
import com.carrental.system.usedCar.repository.UsedCarRepository;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	public abstract class SalesRecordMapper {

	    @Autowired 
	    protected UsedCarRepository usedCarRepo;
	    
	    @Autowired 
	    protected CustomerRepository customerRepo;
	    
	    @Autowired 
	    protected EmployeeRepository employeeRepo;

	    @Mapping(target = "usedCar", source = "usedCarId", qualifiedByName = "idToUsedCar")
	    @Mapping(target = "customer", source = "custId", qualifiedByName = "idToCustomer")
	    @Mapping(target = "emp", source = "empId", qualifiedByName = "idToEmployee")
	    public abstract SalesRecordBean toEntity(SalesRecordDto dto);

	    @Mapping(target = "usedCarId", source = "usedCar.usedCarId")
	    @Mapping(target = "custId", source = "customer.custId")
	    @Mapping(target = "empId", source = "emp.empId")
	    public abstract SalesRecordDto toDto(SalesRecordBean bean);
	    
	    @Mapping(target = "usedCar", source = "usedCarId", qualifiedByName = "idToUsedCar")
	    @Mapping(target = "customer", source = "custId", qualifiedByName = "idToCustomer")
	    @Mapping(target = "emp", source = "empId", qualifiedByName = "idToEmployee")
	    public abstract void updateSalesRecordFromDto(SalesRecordDto dto, @MappingTarget SalesRecordBean entity);

	    // --- Helper Methods ---
	    @Named("idToUsedCar")
	    protected UsedCarBean idToUsedCar(Integer id) {
	        return id == null ? null : usedCarRepo.findById(id).orElse(null);
	    }

	    @Named("idToCustomer")
	    protected CustomerBean idToCustomer(Integer id) {
	        return id == null ? null : customerRepo.findById(id).orElse(null);
	    }

	    @Named("idToEmployee")
	    protected EmployeeBean idToEmployee(Integer id) {
	        return id == null ? null : employeeRepo.findById(id).orElse(null);
	    }
	}