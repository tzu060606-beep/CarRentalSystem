package com.carrental.system.usedCar.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.carrental.system.usedCar.DTO.ViewingAppointmentDto;
import com.carrental.system.usedCar.entity.UsedCarBean;
import com.carrental.system.usedCar.entity.ViewingAppointmentBean;
import com.carrental.system.usedCar.entity.ViewingAppointmentStatus;
import com.carrental.system.usedCar.mapper.ViewingAppointmentMapper;
import com.carrental.system.usedCar.repository.ViewingAppointmentRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class ViewingAppointmentService {

	@Autowired
	private ViewingAppointmentRepository viewRepos;

	@Autowired
	private ViewingAppointmentMapper viewMapper;

	@Autowired
	private UsedCarService usedCarService;

	public ViewingAppointmentDto insert(ViewingAppointmentDto dto) {
		// --- (執行所有檢查) ---

		// A. 檢核日期
		checkDateLogic(dto);

		// B. 取得並檢查車輛狀態 (Active 檢查)
		UsedCarBean carEntity = usedCarService.findEntityById(dto.getUsedCarId());
		if (carEntity.getStatus() == null || !"ACTIVE".equals(carEntity.getStatus().name())) {
			throw new RuntimeException("預約失敗：該車輛狀態必須為 ACTIVE，目前狀態為: " + carEntity.getStatus());
		}

		// C. 衝突檢測 (時間軸重疊檢查)
		LocalDateTime startTime = dto.getApptTime().minusHours(1);
		LocalDateTime endTime = dto.getApptTime().plusHours(1);
		if (viewRepos.isTimeSlotOccupied(dto.getUsedCarId(), startTime, endTime)) {
			throw new RuntimeException("該時段前後 1 小時內已有其他預約，請選擇其他時間。");
		}

		// --- 2. 轉換與持久化 ---

		// 這裡使用 Mapper 轉換基本資料，並手動補上關聯的 Entity
		ViewingAppointmentBean bean = viewMapper.toEntity(dto);
		bean.setUsedCar(carEntity); // 綁定真實的 Entity 關聯
		bean.setStatus(ViewingAppointmentStatus.PENDING); // 強制預設狀態

		// 💡 新增：儲存成功後，回傳的 DTO 同樣補上計算人數
		ViewingAppointmentBean savedBean = viewRepos.save(bean);
		return enrichDtoWithQueue(savedBean);
	}

	public ViewingAppointmentDto update(Integer id, ViewingAppointmentDto dto) {
//		checkDateLogic(dto);
		return viewRepos.findById(id).map(existingView -> {
			// 使用 MapStruct 自動將 updatedData 中非 null 的欄位複製到 existingRecord
			viewMapper.updateViewingAppointmentFromDto(dto, existingView);
			ViewingAppointmentBean updateBean = viewRepos.save(existingView);
			// 儲存更新後的持久化物件
			// 💡 修改：更新後回傳也補上人數計算
			return enrichDtoWithQueue(updateBean);
		}).orElseThrow(() -> new RuntimeException("找不到ID:" + id));
	}

	public void deleteById(Integer id) {
		viewRepos.deleteById(id);
		System.out.println("delete OK");
	}

	public ViewingAppointmentDto selectById(Integer id) {
		// 💡 修改：單筆查詢時，透過自訂方法補上 queueCount
		return viewRepos.findById(id).map(this::enrichDtoWithQueue).orElse(null);
	}

	public List<ViewingAppointmentDto> findAll() {
		List<ViewingAppointmentBean> beans = viewRepos.findAll();
		// 💡 修改：全量查詢時，逐筆補上 queueCount
		return beans.stream().map(this::enrichDtoWithQueue).toList();
	}

	public List<ViewingAppointmentDto> findByCustId(Integer customerId) {
		List<ViewingAppointmentBean> list = viewRepos.findByCustomerCustId(customerId);
		// 💡 修改：客戶專屬查詢（前端 Vue 呼叫的 API），逐筆計算排隊人數並塞入 DTO
		return list.stream().map(this::enrichDtoWithQueue).collect(Collectors.toList());
	}

	private void checkDateLogic(ViewingAppointmentDto dto) {
		LocalDateTime today = LocalDateTime.now();
		if (dto.getApptTime() != null && dto.getApptTime().isBefore(today)) {
			throw new RuntimeException("預約時間不可為今日以前");
		}
	}

	/**
	 * 💡 核心新增：專屬封裝工具（資工重構優化） 作用：將傳入的 Bean 轉成 DTO 後，去資料庫數出「看車時間比這筆早」的人數，塞進 DTO 中回傳。
	 */
	private ViewingAppointmentDto enrichDtoWithQueue(ViewingAppointmentBean bean) {
		// 1. 先用原本的 MapStruct 轉成基礎 DTO 郵包
		ViewingAppointmentDto dto = viewMapper.toDto(bean);

		// 2. 如果該預約有對應車輛與看車時間，就去資料庫計算順位人數
		if (bean.getUsedCar() != null && bean.getApptTime() != null) {
			long count = viewRepos.countPeopleAheadByApptTime(bean.getUsedCar().getUsedCarId(), bean.getApptTime());
			// 3. 把計算結果塞給剛才在 DTO 擴充的 queueCount 屬性
			dto.setQueueCount((int) count);
		} else {
			dto.setQueueCount(0);
		}

		return dto;
	}
	/**
	 * 💡 步驟 4 修改版：保留原本備註並向下追加文字
	 */
	@org.springframework.context.event.EventListener
	public void onCarSoldCancelAppointments(com.carrental.system.usedCar.service.CarSoldEvent event) {
		Integer soldCarId = event.getUsedCarId();
		System.out.println("【預約模組收到廣播】偵測到車輛 ID: " + soldCarId + " 已成交。開始自動清理無效預約...");

		// 1. 撈出這台車所有還在排隊的有效預約單
		List<ViewingAppointmentBean> activeAppts = viewRepos.findActiveAppointmentsByCarId(soldCarId);

		if (activeAppts != null && !activeAppts.isEmpty()) {
			// 2. 逐筆將狀態改為 CANCELLED，並「追加」備註
			for (ViewingAppointmentBean appt : activeAppts) {
				appt.setStatus(ViewingAppointmentStatus.CANCELLED);
				
				// 💡 核心修改：先取得原本的備註文字
				String originalNotes = appt.getNotes();
				String systemNotice = "[系統自動審核] 因該車輛已售出結案，系統自動取消此預約。";
				
				// 💡 邏輯判斷：如果原本沒字，直接塞系統通知；如果有字，就「換行 (\n)」再補上系統通知
				if (originalNotes == null || originalNotes.trim().isEmpty()) {
					appt.setNotes(systemNotice);
				} else {
					appt.setNotes(originalNotes + "\n" + systemNotice);
				}
			}

			// 3. 批次存回資料庫
			viewRepos.saveAll(activeAppts);
			System.out.println("【清理成功】已自動取消該車輛共 " + activeAppts.size() + " 筆預約單。");
		} else {
			System.out.println("【清理結束】該車輛目前沒有其他排隊預約，無需處置。");
		}
	}

}
