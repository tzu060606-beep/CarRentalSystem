package com.carrental.system.vehicle.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carrental.system.vehicle.entity.CrossLocationFee;
import com.carrental.system.vehicle.exception.CustomValidationException;
import com.carrental.system.vehicle.service.CrossLocationFeeService;

@RestController
@RequestMapping("/api/crosslocationfee")
@CrossOrigin
public class CrossLocationFeeController {

	private CrossLocationFeeService crossLocationFeeService;
	
	@Autowired
	public CrossLocationFeeController (CrossLocationFeeService crossLocationFeeService) {
		this.crossLocationFeeService = crossLocationFeeService;
	}

//	================== 一、對外 API ==================
 	
//	==================== 二、CRUD ====================
	
//	查單筆(by調度起訖據點id)
	@GetMapping("/{fromId}/{toId}")
	public ResponseEntity<BigDecimal> calculateFee(
			@PathVariable("fromId") Integer fromLocationId,
			@PathVariable("toId") Integer toLocationId){
		BigDecimal fee = crossLocationFeeService.getFee(fromLocationId, toLocationId);
		return ResponseEntity.ok(fee);
	}
	
//	查單筆
	@GetMapping("/{feeId}")
	public CrossLocationFee getCrossLocationFee(@PathVariable ("feeId") Integer feeId) {
		return crossLocationFeeService.getCrossLocationFeeById(feeId);
	}
//	查全部
	@GetMapping
	public List<CrossLocationFee> listCrossLocationFees(){
		return crossLocationFeeService.getCrossLocationFees();
	}
//	增
	@PostMapping
	public CrossLocationFee addCrossLocationFee(@RequestBody CrossLocationFee crossLocationFee) {
		
		Map<String, String> errors = new HashMap<>();
		if (!errors.isEmpty()) {
			throw new CustomValidationException(errors);
		}
		return crossLocationFeeService.createCrossLocationFee(crossLocationFee);
	}
//	改
	@PutMapping("/{feeId}")
	public CrossLocationFee editCrossLocationFee(
			@PathVariable ("feeId") Integer feeId,
			@RequestBody CrossLocationFee crossLocationFee) {
		crossLocationFee.setFeeId(feeId);
		return crossLocationFeeService.updateCrossLocationFee(crossLocationFee);
	}
//	刪
	@DeleteMapping("/{feeId}")
	public String removeCrossLocationFee(@PathVariable ("feeId") Integer feeId) {
		CrossLocationFee crossLocationFee = crossLocationFeeService.getCrossLocationFeeById(feeId);
		if (crossLocationFee != null) {
			crossLocationFeeService.deleteCrossLocationFee(feeId);
			return "刪除成功";
		}
		return "查無此ID對應異地調度費率，無法刪除";
	}
}
