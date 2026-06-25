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

import com.carrental.system.usedCar.DTO.ViewingAppointmentDto;
import com.carrental.system.usedCar.service.ViewingAppointmentService;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/viewingappointment")
public class ViewingAppointmentController {

	@Autowired
	private ViewingAppointmentService viewService;

	@GetMapping("/getall")
	public List<ViewingAppointmentDto> getAll() {
		return viewService.findAll();
	}

	@GetMapping("/{id}")
	public ViewingAppointmentDto getById(@PathVariable Integer id) {
		return viewService.selectById(id);
	}

	@DeleteMapping("/{id}")
	public List<ViewingAppointmentDto> deleteById(@PathVariable Integer id) {
		viewService.deleteById(id);
		return viewService.findAll();
	}

	@PutMapping("/{id}")
	public List<ViewingAppointmentDto> update(@PathVariable Integer id,
			@RequestBody ViewingAppointmentDto viewDto) {
		viewService.update(id,viewDto);
		return viewService.findAll();
	}

	@PostMapping("/insert")
	public List<ViewingAppointmentDto> insert(@RequestBody ViewingAppointmentDto viewDto) {
		viewService.insert(viewDto);
        return viewService.findAll();
	}
	
	// 根據顧客 ID 查詢所有預約
    @GetMapping("/customer/{custId}")
    public ResponseEntity<List<ViewingAppointmentDto>> findByCustId(@PathVariable Integer custId) {
        List<ViewingAppointmentDto> list = viewService.findByCustId(custId);
        
        // 判斷是否有資料，沒有則回傳 404 或空列表 200
        if (list.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(list);
    }

}
