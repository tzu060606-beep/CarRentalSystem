package com.carrental.system.vehicle.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carrental.system.vehicle.entity.CrossLocationFee;
import com.carrental.system.vehicle.exception.CustomValidationException;
import com.carrental.system.vehicle.repository.CrossLocationFeeRepository;

@Service
@Transactional
public class CrossLocationFeeService {

	private CrossLocationFeeRepository crossLocationFeeRepository;
	
	@Autowired
	public CrossLocationFeeService(CrossLocationFeeRepository crossLocationFeeRepository) {
		this.crossLocationFeeRepository = crossLocationFeeRepository;
	}
	
//	================== 一、對外 API ==================
	public BigDecimal getFee(Integer fromLocationId, Integer toLocationId) {
		
		// 1. 防呆：若同地還車，費用為0
		if (fromLocationId.equals(toLocationId)) {
			return BigDecimal.ZERO;
		}
		// 2. 若確為異地，去資料庫找實體，並取其「extraFee」欄位
		Optional<CrossLocationFee> selectedFee = crossLocationFeeRepository.findFeeByLocationIds(fromLocationId, toLocationId);
		// 
		if (selectedFee.isPresent()) {
			return selectedFee.get().getExtraFee();
		}
		throw buildCustomValidationException("crossLocationFee", "查無此路線對應異地調度費用");
	}
	
//	==================== 二、CRUD ====================
	
//	查單筆
	public CrossLocationFee getCrossLocationFeeById(Integer feeId) {
		Optional<CrossLocationFee> crossLoactionFee = crossLocationFeeRepository.findById(feeId);
		if (crossLoactionFee.isPresent()) {
			return crossLoactionFee.get();
		}
		return null;
	}
	
//	查全部
	public List<CrossLocationFee> getCrossLocationFees(){
		return crossLocationFeeRepository.findAll();
	}
	
//	增
	public CrossLocationFee createCrossLocationFee(CrossLocationFee crossLocationFee) {
		return crossLocationFeeRepository.save(crossLocationFee);
	}
	
//	改
	public CrossLocationFee updateCrossLocationFee(CrossLocationFee crossLocationFee) {
		return crossLocationFeeRepository.save(crossLocationFee);
	}
	
//	刪
	public boolean deleteCrossLocationFee(Integer feeId) {
		crossLocationFeeRepository.deleteById(feeId);
		return true;
	}
	
//	================ 三、本身商業邏輯 ================
	
	
//	================ 四、私有輔助方法 ================
	
//	輔助方法1：拋出CustomValidationException
	private void throwCustomValidationException(String fieldName, String errorString) {
		Map<String, String> errors = new HashMap<String, String>();
		errors.put(fieldName, errorString);
		throw new CustomValidationException(errors);
	}
	
//	輔助方法2：回傳CustomValidationException
	private CustomValidationException buildCustomValidationException(String fieldName, String errorString) {
		Map<String, String> errors = new HashMap<String, String>();
		errors.put(fieldName, errorString);
		return new CustomValidationException(errors);
	}
	
}
