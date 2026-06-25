package com.carrental.system.vehicle.controller;

import com.carrental.system.vehicle.entity.CrossLocationFee;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carrental.system.vehicle.entity.CarModel;
import com.carrental.system.vehicle.exception.CustomValidationException;
import com.carrental.system.vehicle.service.CarModelService;

@RestController
@RequestMapping("/api/carmodel")
@CrossOrigin
public class CarModelController {

	private CarModelService carModelService;
	
	@Autowired //建構子注入
	public CarModelController(CarModelService carModelService) {
		this.carModelService = carModelService;
	}
	
//	查單筆
	@GetMapping("/{carModelId}")
	public CarModel getCarModel(@PathVariable("carModelId") Integer carModelId) {
		CarModel resultModel = carModelService.getModelById(carModelId);
		return resultModel;
	}
	
//	查全部
	@GetMapping
	public List<CarModel> listCarModels(){
		return carModelService.getAllModels();
	}
	
//	增
	@PostMapping
	public String addCarModel(@RequestBody CarModel carModel) {
		
		// 錯誤收集器
		Map<String, String> errors = new HashMap<String, String>();
		
		// 1. 必填：brand/modelName/vehicleType/seats
		requiredNotBlank(errors, "brand", carModel.getBrand(), "品牌為必填，不可空白");
		requiredNotBlank(errors, "modelName", carModel.getModelName(), "型號為必填，不可空白");
		requiredNotNull(errors, "displacement", carModel.getDisplacement(), "排氣量為必填，不可空白");
		requiredNotBlank(errors, "vehicleType", carModel.getVehicleType(), "車型為必填，不可空白");
		requiredNotNull(errors, "seats", carModel.getSeats(), "座位數為必填，不可空白");
	
		
		// 若有錯誤，直接丟到exception class回傳錯誤訊息到前端
		if (!errors.isEmpty()) {
			throw new CustomValidationException(errors);
		}	
		// 確認通過驗證，才放行到service
		carModelService.createModel(carModel);
		return "新增成功";
	}
	
//	改
	@PutMapping("/{carModelId}")
	public String editCarModel(
			@PathVariable("carModelId") Integer modelId, 
			@RequestBody CarModel carModel) {
		carModel.setModelId(modelId);
		carModelService.updateModel(carModel);
		return "修改成功";
	}

//	刪
	@DeleteMapping("/{carModelId}")
	public String removeCarModel(@PathVariable("carModelId") Integer carModelId) {
		CarModel deleteModel = carModelService.getModelById(carModelId);
		if (deleteModel != null) {
			carModelService.deleteModel(carModelId);
			return "刪除成功";
		}
		return "查無此ID車款，無法刪除";
	}
	
//	查單筆（含已軟刪除）
	@GetMapping("/all/{carModelId}")
	public CarModel getCarModelIncludingDeleted(@PathVariable("carModelId") Integer modelId) {
		return carModelService.getModelByIdIncludingDeleted(modelId);
	}
	
//	查全部「包含已軟刪除」
	@GetMapping("/all")
	public List<CarModel> listCarModelsIncludingDeleted(){
		return carModelService.getAllModelsIncludingDeleted();
	}
	
//	輔助方法：「字串」必填防空白 參數(map收集器, 欄位名, 前端回傳值, 錯誤訊息)
	private void requiredNotBlank(Map<String, String> errors, String fieldName, String value, String message) {
		if (value == null || value.trim().isEmpty()) {
			errors.put(fieldName, message);
		}
	}
//	輔助方法：「數字或物件」必填
	private void requiredNotNull(Map<String, String> errors, String fieldName, Object value, String message) {
		if (value == null) {
			errors.put(fieldName, message);
		}
	}
	
}
