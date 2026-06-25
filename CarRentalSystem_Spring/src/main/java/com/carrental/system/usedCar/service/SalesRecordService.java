package com.carrental.system.usedCar.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.carrental.system.usedCar.DTO.SalesRecordDto;
import com.carrental.system.usedCar.entity.SalesRecordBean;
import com.carrental.system.usedCar.entity.UsedCarStatus;
import com.carrental.system.usedCar.mapper.SalesRecordMapper;
import com.carrental.system.usedCar.repository.SalesRecordRepository;
import com.carrental.system.usedCar.repository.UsedCarRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class SalesRecordService {

	@Autowired
	private SalesRecordRepository salesRepos;

	@Autowired
	private SalesRecordMapper salesMapper;
	
	@Autowired
	private UsedCarRepository usedCarRepos;
	
	@Autowired
	private ApplicationEventPublisher eventPublisher; // 💡 3. 注入 Spring 內建的廣播物件

	public SalesRecordDto insert(SalesRecordDto dto) {
		checkDateLogic(dto);
		SalesRecordBean bean = salesMapper.toEntity(dto);
		SalesRecordBean savedBean = salesRepos.save(bean);
		
		//取得成交ID
		Integer usedCarId = dto.getUsedCarId();
		
		//找二手車
		usedCarRepos.findById(usedCarId).ifPresent(usedCar -> {
			//改狀態SOLD
			usedCar.setStatus(UsedCarStatus.SOLD);
			//存回二手車
			usedCarRepos.save(usedCar);
			// 💡 4. 在這裡直球發射廣播！
			// 當資料庫確定把這台車改成 SOLD 之後，立刻對整個 Spring 系統大喊這台車賣掉了
			System.out.println("【成交模組廣播】車輛 ID: " + usedCarId + " 已成功寫入成交表並改為 SOLD。發射事件！");
			eventPublisher.publishEvent(new CarSoldEvent(usedCarId));
		});
		
		return salesMapper.toDto(savedBean);
	}

	public SalesRecordDto update(Integer id, SalesRecordDto dto) {
	    checkDateLogic(dto);
	    return salesRepos.findById(id).map(existingRecord -> {
	        // 1. 取得更新前的狀態
	        String oldPayStatus = existingRecord.getPayStatus() != null ? existingRecord.getPayStatus().name() : "NULL";
	        
	        // 2. 更新資料
	        salesMapper.updateSalesRecordFromDto(dto, existingRecord);
	        SalesRecordBean updateBean = salesRepos.save(existingRecord);
	        
	        // 3. 取得更新後的狀態
	        String currentStatus = updateBean.getPayStatus() != null ? updateBean.getPayStatus().name() : "NULL";
	        
	        // 4. 判斷是否觸發事件 (將偵錯與邏輯合併)
	        if (!"CANCELLED".equals(oldPayStatus) && "CANCELLED".equals(currentStatus)) {
	            System.out.println("【成交模組】偵測到狀態變更為 CANCELLED，發射 CarReturnedEvent");
	            
	            // 取得車輛ID並發射
	            Integer carId = updateBean.getUsedCar().getUsedCarId(); 
	            eventPublisher.publishEvent(new UsedCarService.CarReturnedEvent(carId));
	        } else {
	            System.out.println("【成交模組】無須發射事件 (舊狀態: " + oldPayStatus + ", 新狀態: " + currentStatus + ")");
	        }
	        
	        return salesMapper.toDto(updateBean);
	    }).orElseThrow(() -> new RuntimeException("找不到銷售紀錄 ID: " + id));
	}

	public void deleteById(Integer id) {
		salesRepos.deleteById(id);
		System.out.println("delete OK");
	}

	public SalesRecordDto selectById(Integer id) {
		return salesRepos.findById(id).map(salesMapper::toDto).orElse(null);
	}

	public List<SalesRecordDto> findAll() {
		List<SalesRecordBean> beans = salesRepos.findAll();
		return beans.stream().map(salesMapper::toDto).toList();
	}
	
	public List<SalesRecordDto> findByCustId(Integer customerId){
		List<SalesRecordBean> list = salesRepos.findByCustomerCustId(customerId);
		return list.stream().map(salesMapper::toDto).collect(Collectors.toList());
	}
	
	private void checkDateLogic(SalesRecordDto dto) {
		LocalDate today = LocalDate.now();
		if(dto.getSaleDate() != null && dto.getSaleDate().isAfter(today)) {
			throw new RuntimeException("成交日不能晚於今日");
		}
	}

}
