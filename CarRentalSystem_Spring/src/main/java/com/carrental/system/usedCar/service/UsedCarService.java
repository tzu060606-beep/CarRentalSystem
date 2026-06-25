package com.carrental.system.usedCar.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.carrental.system.usedCar.DTO.UsedCarDto;
import com.carrental.system.usedCar.DTO.UsedCarVehicleDetailDto;
import com.carrental.system.usedCar.entity.UsedCarBean;
import com.carrental.system.usedCar.entity.UsedCarStatus;
import com.carrental.system.usedCar.mapper.UsedCarMapper;
import com.carrental.system.usedCar.mapper.UsedCarVehicleDetailMapper;
import com.carrental.system.usedCar.repository.UsedCarRepository;
import com.carrental.system.vehicle.entity.Vehicle;
import com.carrental.system.vehicle.entity.VehicleStatus;
import com.carrental.system.vehicle.exception.CustomValidationException;
import com.carrental.system.vehicle.repository.VehicleRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class UsedCarService {

	@Autowired
	private UsedCarRepository usedCarRepos;

	@Autowired
	private UsedCarMapper usedCarMapper;

	@Autowired
	private UsedCarVehicleDetailMapper detailMapper;

	@Autowired
	private VehicleRepository vehicleRepository;
	
	// 💡 直接把 Event 定義成 static inner class，不用額外開檔案
    public static class CarReturnedEvent {
        private final Integer usedCarId;
        public CarReturnedEvent(Integer usedCarId) { this.usedCarId = usedCarId; }
        public Integer getUsedCarId() { return usedCarId; }
    }

    // 💡 直接在這裡監聽，當 SalesRecordService 發射事件時就會觸發
    @EventListener
    @Transactional
    public void handleCarReturnedEvent(CarReturnedEvent event) {
        System.out.println("【自動連動】接收到取消通知，準備恢復車輛狀態...");
        updateStatus(event.getUsedCarId(), UsedCarStatus.REMOVED);
    }

	public UsedCarDto insert(UsedCarDto dto) {
		// 1. 先確認該車輛是否存在，並取得其資訊
		// 假設 UsedCarDto 中有 vehicleId，請根據你的實際欄位名稱調整
		Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
				.orElseThrow(() -> new RuntimeException("找不到車輛編號: " + dto.getVehicleId()));

		if (vehicle.getStatus() == null || vehicle.getStatus() != VehicleStatus.RETIRED) {
			// 這裡可以將錯誤綁定在 vehicleId 欄位上
			Map<String, String> errors = new HashMap<>();
			errors.put("vehicleId", "只有狀態為 [RETIRED] 的車輛才能上架。目前狀態: " + vehicle.getStatus());
			throw new CustomValidationException(errors);
		}

		if (dto.getAskingPrice().compareTo(BigDecimal.ZERO) <= 0) {
			Map<String, String> errors = new HashMap<>();
			errors.put("askingPrice", "預售價格不得小於0");
			throw new CustomValidationException(errors);
		}
		checkDateLogic(dto);
		UsedCarBean bean = usedCarMapper.toEntity(dto);
		UsedCarBean savedBean = usedCarRepos.save(bean);
		return usedCarMapper.toDto(savedBean); // 回傳 DTO
	}

	// 修改
	public UsedCarDto update(Integer id, UsedCarDto dto) {
		checkDateLogic(dto);
		return usedCarRepos.findById(id).map(existingcar -> {
			usedCarMapper.updateUsedCarFromDto(dto, existingcar);
			UsedCarBean updatedBean = usedCarRepos.save(existingcar);
			return usedCarMapper.toDto(updatedBean); // 回傳 DTO
		}).orElseThrow(() -> new RuntimeException("找不到ID:" + id));
	}

	public void deleteById(Integer id) {
		usedCarRepos.deleteById(id);
		System.out.println("delete OK");
	}

	// 查詢單筆：回傳 DTO
	public UsedCarDto selectById(Integer id) {
		return usedCarRepos.findById(id).map(usedCarMapper::toDto) // 將 Bean 轉成 Dto
				.orElse(null);
	}

	// 查詢全部：回傳 List<UsedCarDto>
	public List<UsedCarDto> findAll() {
		List<UsedCarBean> beans = usedCarRepos.findAll();
		
		// 在轉換成 DTO 之前，先檢查每一台車是否過期
		beans.forEach(this::syncExpireStatus);
		// 使用 Stream API 把每一筆 Bean 都轉成 Dto
		return beans.stream().map(usedCarMapper::toDto).toList();
	}

	public UsedCarVehicleDetailDto getUsedCarVehicleDetailById(Integer usedCarId) {
		// 1. 從資料庫撈出 UsedCarBean (這會連帶抓出 Vehicle, CarModel, Location)
		UsedCarBean usedCar = usedCarRepos.findById(usedCarId)
				.orElseThrow(() -> new RuntimeException("找不到編號為 " + usedCarId + " 的二手車資料"));
		// 在轉換成 DTO 之前，先檢查每一台車是否過期
		
		// 2. 丟給 Mapper 轉成我們定義好的平坦化 DTO
		// 這裡 Mapper 會幫你執行那 20 幾個 @Mapping 邏輯
		return detailMapper.toVehicleDetailDto(usedCar);
	}

	public List<UsedCarVehicleDetailDto> getUsedCarVehicleDetail() {
		// 1. 從資料庫查詢所有二手車實體 (Entity)
		List<UsedCarBean> usedCars = usedCarRepos.findAll();

		// 在轉換成 DTO 之前，先檢查每一台車是否過期
		usedCars.forEach(this::syncExpireStatus);

		// 2. 使用 Java Stream 與 Mapper 進行轉換
		// 這裡會自動處理你在 Mapper 裡定義的所有深層對應 (vehicle, model, location)
		return usedCars.stream().map(detailMapper::toVehicleDetailDto).collect(Collectors.toList());
	}

	public UsedCarVehicleDetailDto listAvailable(Integer usedCarId) {
		UsedCarBean usedCar = usedCarRepos.findById(usedCarId)
				.orElseThrow(() -> new RuntimeException("找不到編號為 " + usedCarId + " 的資料"));
		return detailMapper.toVehicleDetailDto(usedCar);

	}

	private void checkDateLogic(UsedCarDto dto) {
		LocalDate today = LocalDate.now();
		// 刊登截止日不能在今天以前
		Map<String, String> errors = new HashMap<>();

		if (dto.getExpireDate() != null && dto.getExpireDate().isBefore(today)) {
			errors.put("expireDate", "刊登截止日不可早於今日");
		}

		if (dto.getListDate() != null && dto.getExpireDate() != null
				&& dto.getListDate().isAfter(dto.getExpireDate())) {
			errors.put("listDate", "上架日期不可晚於刊登截止日");
		}

		// 如果錯誤 Map 不為空，就拋出異常，Handler 會自動幫你轉成 JSON 回傳
		if (!errors.isEmpty()) {
			throw new CustomValidationException(errors);
		}
	}

	// 在 UsedCarService.java 中新增一個方法
	public UsedCarBean findEntityById(Integer id) {
		// 直接從 Repository 撈出 Entity，這樣 JPA 才能綁定關聯
		return usedCarRepos.findById(id).orElseThrow(() -> new RuntimeException("找不到車輛編號: " + id));
	}

	// 截止刊登日到期後自動驗證下架
	private void syncExpireStatus(UsedCarBean bean) {

		// 取得最新日期
		LocalDate today = LocalDate.now();

		// 如果 狀態是上架中 且 今天已經超過截止日
		if (bean.getStatus() == UsedCarStatus.ACTIVE 
				&& bean.getExpireDate() != null
				&& bean.getExpireDate().isBefore(today)) {

			bean.setStatus(UsedCarStatus.REMOVED); // 這裡建議用你的 Enum: UsedCarStatus.ARCHIVED
			usedCarRepos.save(bean); // 立即同步回資料庫
		}
	}
	
	/**
	 * 專門給成交單系統呼叫，用來強制變更車輛狀態
	 */
	public void updateStatus(Integer usedCarId, UsedCarStatus newStatus) {
	    usedCarRepos.findById(usedCarId).ifPresent(car -> {
	        car.setStatus(newStatus);
	        usedCarRepos.save(car);
	        System.out.println("【車輛系統】車輛 ID: " + usedCarId + " 狀態已更新為: " + newStatus);
	    });
	}

}
