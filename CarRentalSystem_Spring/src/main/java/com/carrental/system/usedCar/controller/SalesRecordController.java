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

import com.carrental.system.usedCar.DTO.SalesRecordDto;
import com.carrental.system.usedCar.service.SalesRecordService;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/salesrecord")
public class SalesRecordController {

	@Autowired
	private SalesRecordService salesService;
	
	@GetMapping("/getall")
	public List<SalesRecordDto> getAll(){
		return salesService.findAll();
	}
	
	@GetMapping("/{id}")
	public SalesRecordDto getById(@PathVariable Integer id) {
		return salesService.selectById(id);
	}
	
	@DeleteMapping("/{id}")
	public List<SalesRecordDto> deleteById(@PathVariable Integer id) {
		salesService.deleteById(id);
		return salesService.findAll();
	}
	
	@PutMapping("/{id}")
	public List<SalesRecordDto> update(@PathVariable Integer id, @RequestBody SalesRecordDto salesRecord) {
	    // 呼叫修改後的 service 方法，傳入 ID 與 RequestBody 物件
	    salesService.update(id, salesRecord);
	    
	    // 更新成功後，回傳清單（如你原有的邏輯）
	    return salesService.findAll();
	}
	
	@PostMapping("/insert")
	public List<SalesRecordDto> insert(@RequestBody SalesRecordDto salesRecord){
		salesService.insert(salesRecord);
		return salesService.findAll();
	}
	
	// 根據顧客 ID 查詢所有預約
    @GetMapping("/customer/{custId}")
    public ResponseEntity<List<SalesRecordDto>> findByCustId(@PathVariable Integer custId) {
        List<SalesRecordDto> list = salesService.findByCustId(custId);
        
        // 判斷是否有資料，沒有則回傳 404 或空列表 200
        if (list.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(list);
    }
	
}
