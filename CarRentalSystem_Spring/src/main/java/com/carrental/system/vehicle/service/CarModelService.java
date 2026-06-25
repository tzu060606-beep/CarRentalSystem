package com.carrental.system.vehicle.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carrental.system.vehicle.entity.CarModel;
import com.carrental.system.vehicle.repository.CarModelRepository;


@Service @Transactional
public class CarModelService {

	private CarModelRepository carModelRepository;
	
	@Autowired //建構子注入
	public CarModelService(CarModelRepository carModelRepository) {
		this.carModelRepository = carModelRepository;
	}
	
//	查單筆
	public CarModel getModelById(Integer carModelId) {
		Optional<CarModel> carModel = carModelRepository.findById(carModelId);
		if (carModel.isPresent()) {
			return carModel.get();
		}
		return null;
	}
	
//	查全部
	public List<CarModel> getAllModels(){
		return carModelRepository.findAll();
	}
	
//	增
	public CarModel createModel(CarModel carModel) {
		return carModelRepository.save(carModel);
	}
	
//	改
	public CarModel updateModel(CarModel carModel) {
		return carModelRepository.save(carModel);
	}
	
//	刪
	public String deleteModel(Integer carModelId) {
		carModelRepository.deleteById(carModelId);
		return "刪除成功";
	}
	
//	查單筆（含已軟刪除）
	public CarModel getModelByIdIncludingDeleted(Integer modelId) {
		Optional<CarModel> selectedModel = carModelRepository.findByIdIncludingDeleted(modelId);
		if (selectedModel.isPresent()) {
			return selectedModel.get();
		}
		return null;
	}
	
//	查全部（含已軟刪除）
	public List<CarModel> getAllModelsIncludingDeleted(){
		return carModelRepository.findAllIncludingDeleted();
	}
	
}
