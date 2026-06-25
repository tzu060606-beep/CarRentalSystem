package com.carrental.system.usedCar.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

import com.carrental.system.usedCar.DTO.UsedCarDto;
import com.carrental.system.usedCar.DTO.UsedCarVehicleDetailDto;
import com.carrental.system.usedCar.service.UsedCarService;

@RestController
@CrossOrigin(origins = "http://localhost:5173") // 允許來自 Vue 開發環境的請求
@RequestMapping("/api/usedCars")
public class UsedCarController {

	@Autowired
	private UsedCarService usedCarService;

	// http://localhost:8081/usedcar.controller
	@GetMapping("/getall")
	public List<UsedCarDto> getAllCars() {
		return usedCarService.findAll();
	}

	@GetMapping("/{id}")
	public ResponseEntity<UsedCarDto> getCarById(@PathVariable Integer id) {
	    UsedCarDto car = usedCarService.selectById(id);
	    
	    // 這裡我們是用 ResponseEntity「包裝」了 DTO
	    if (car == null) {
	        return ResponseEntity.notFound().build(); // 回傳 404 (沒有 DTO)
	    }
	    
	    return ResponseEntity.ok(car); // 回傳 200，並把 DTO 當作 body 放進去
	}

	@DeleteMapping("/{id}")
	public List<UsedCarDto> deleteCarById(@PathVariable Integer id) {
		usedCarService.deleteById(id);
		return usedCarService.findAll();
	}

	@PutMapping("/{id}")
	public List<UsedCarDto> updateCar(@PathVariable Integer id, @RequestBody UsedCarDto usedcarDto) {
		usedCarService.update(id, usedcarDto);
		return usedCarService.findAll();
	}

	@PostMapping("/insert")
	public List<UsedCarDto> insertCar(@RequestBody UsedCarDto usedcarDto) {
		usedCarService.insert(usedcarDto);
		return usedCarService.findAll();
	}

	@GetMapping("/detail/{id}")
    public ResponseEntity<UsedCarVehicleDetailDto> getUsedCarDetail(@PathVariable Integer id) {
        UsedCarVehicleDetailDto detail = usedCarService.getUsedCarVehicleDetailById(id);
        return ResponseEntity.ok(detail);
    }
	
	@GetMapping("/details") // 前端 getAllDetails() 呼叫的路徑
	public ResponseEntity<List<UsedCarVehicleDetailDto>> getAllUsedCarDetails() {
	    List<UsedCarVehicleDetailDto> list = usedCarService.getUsedCarVehicleDetail();
	    if (list.isEmpty()) {
	        return ResponseEntity.noContent().build();
	    }
	    return ResponseEntity.ok(list);
	}
	
	
	
}