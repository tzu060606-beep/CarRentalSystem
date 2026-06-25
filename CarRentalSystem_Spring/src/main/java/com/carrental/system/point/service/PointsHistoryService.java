package com.carrental.system.point.service;

import com.carrental.system.login.entity.CustomerBean;
import com.carrental.system.login.repository.CustomerRepository;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

import com.carrental.system.point.dto.PointsHistoryResponseDTO;
import com.carrental.system.point.entity.PointsHistory;
import com.carrental.system.point.exception.CustomerNotFoundException;
import com.carrental.system.point.exception.ResourceNotFoundException;
import com.carrental.system.point.repository.PointsHistoryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PointsHistoryService {

	// 建構子注入
	private final PointsHistoryRepository pointsHistoryRepository;
	private final CustomerRepository customerRepository;

	// 查詢特定客戶的點數異動紀錄
	public List<PointsHistoryResponseDTO> findByCustId(Integer custId) {
		  // 查點數異動紀錄
		    return pointsHistoryRepository.findByCustIdWithCustName(custId);
		}
	

	// 新增點數變動紀錄(同時記錄建立時間與計算到期時間)
	public PointsHistory insertPointsHistory(PointsHistory pointsHistory) {
		LocalDateTime now = LocalDateTime.now();
		pointsHistory.setCreateTime(now);
		pointsHistory.setExpireTime(now.plusDays(365));
		
		//[新增]-因應FIFO邏輯，需要更新如果是獲得點數，自動設定 availablePoints
	    if (pointsHistory.getPointsChange() != null && pointsHistory.getPointsChange() > 0) {
	        pointsHistory.setAvailablePoints(pointsHistory.getPointsChange());
	    }
		
		PointsHistory newPointsHistory = pointsHistoryRepository.save(pointsHistory);
		return newPointsHistory;
	}

	// 修改點數變動紀錄
	public PointsHistory updatePointsHistory(Integer id, PointsHistory pointsHistory) {
		Optional<PointsHistory> targetPointsHistory = pointsHistoryRepository.findById(id);
		PointsHistory p = targetPointsHistory.orElseThrow(() -> new ResourceNotFoundException("查無id為" + id + "的點數異動紀錄"));
		// cust_id是會員編號是否應為不可修改?
//			p.setCustId(pointsHistory.getCustId());
		p.setRemainPoints(pointsHistory.getRemainPoints());
		p.setChangeType(pointsHistory.getChangeType());
		p.setPointsChange(pointsHistory.getPointsChange());
		// reference id為關聯用id，是否也應為不可修改
//			p.setReferenceId(pointsHistory.getReferenceId());
		p.setNotes(pointsHistory.getNotes());
		// Create_time為創建當下時間，是否應為不可修改
//			p.setCreateTime(pointsHistory.getCreateTime());
		// ExpireTime不應該被修改，因為它是建立時決定的。
//			p.setExpireTime(pointsHistory.getExpireTime());

		PointsHistory updatePointsHistory = pointsHistoryRepository.save(p);
		return updatePointsHistory;
		// 若有不可修改的欄位，是在Service直接不給更新，還是要在其他地方直接擋下，如Repository?或到controller才擋

	}

	// 刪除點數變動紀錄
	public void deletePointHistoryById(Integer id) {

		pointsHistoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("查無id為" + id + "的點數異動紀錄"));
		pointsHistoryRepository.deleteById(id);
	}

	// 查詢單筆點數變動紀錄
	public PointsHistory findPointsHistoryById(Integer id) {
		Optional<PointsHistory> targetPointHistory = pointsHistoryRepository.findById(id);
		return targetPointHistory.orElseThrow(() -> new ResourceNotFoundException("查無id為" + id + "的點數異動紀錄"));
	}

	// 查詢全部點數變動紀錄
	public List<PointsHistoryResponseDTO> findAllPointsHistories() {
		List<PointsHistoryResponseDTO> pointsHistoriesList = pointsHistoryRepository.findAllWithCustName();
		return pointsHistoriesList;
	}

	// 查詢點數變動紀錄關鍵字
	public List<PointsHistory> findPointsHistoriesByKeyword(String keyword) {
		List<PointsHistory> pointsHistoriesList = pointsHistoryRepository.findByKeyword(keyword);
		return pointsHistoriesList;
	}
	
	//FIFO扣點方法-->會回傳扣完點的客戶現有點數
	//加回傳值原因:
	//1. 呼叫者直接拿到結果，不需要額外的推算
	//2. 回傳值版本可以直接驗證結果
	//3. 符合「告知，不要詢問」原則
	//不驗證原因:insert訂單已經驗證過點數夠不夠了，deductPointsByFIFO 裡不需要再驗證一次。
	public Integer deductPointsByFIFO(Integer custId, Integer pointsToDeduct) {
		//先把原有的要扣點數保留起來
		Integer originalPointsToDeduct = pointsToDeduct;
		//查可用點數 
		//availablePointsByFIFO是一個 List<PointsHistory>，裡面是按時間排好的可用點數紀錄。
		List<PointsHistory> availablePointsListByFIFO = pointsHistoryRepository.findAvailablePointsByFIFO(custId, LocalDateTime.now());
		// 取得客戶現有點數
		//1. 驗證客戶是否存在
		CustomerBean customer = customerRepository.findById(custId)
		    .orElseThrow(() -> new CustomerNotFoundException("客戶不存在"));
		//2. 取得客戶現有點數
		Integer currentPoints = customer.getCurrentPoints();
		
		// 從第一筆開始扣，用迴圈寫
		for (PointsHistory pointsHistory : availablePointsListByFIFO) {
			Integer avaliablePoints = pointsHistory.getAvailablePoints();
			//如果該筆點數異動紀錄剩餘可扣的點數>=要扣的點數-->夠扣
			if (avaliablePoints>=pointsToDeduct) {
				//把可扣的點數減掉要扣的點數，剩下該筆記錄可扣的點數
				Integer pointsLeftToDeduct = avaliablePoints-pointsToDeduct;
				//把剩餘的點數設回該筆紀錄的可扣點數
				avaliablePoints=pointsLeftToDeduct;
				//存回物件中
				pointsHistory.setAvailablePoints(avaliablePoints);//(新值)
				//把有新剩餘點數的點數異動紀錄再存回資料庫
				pointsHistoryRepository.save(pointsHistory);
				// 已經扣完，跳出迴圈
				break;
				
			} else{
				//如果該筆點數異動紀錄剩餘可扣的點數<要扣的點數-->不夠扣
				
				//把可扣的點數減掉要扣的點數，剩下應扣而未扣的點數
				Integer pointsLeftToDeduct = pointsToDeduct-avaliablePoints;//(要反著扣不然會是負值)
				//把剩餘的應扣點數設為下一筆紀錄要扣的點數
				pointsToDeduct=pointsLeftToDeduct;
				//把這筆紀錄的剩餘點數設為0
				avaliablePoints = 0;
				//存回物件中
				pointsHistory.setAvailablePoints(avaliablePoints);//(新值)
				//把有歸零可扣點數的點數異動紀錄再存回資料庫
				pointsHistoryRepository.save(pointsHistory);
			}
			
		}
		// 追蹤客戶剩餘點數
		Integer remainingCurrentPoints=currentPoints-originalPointsToDeduct;
		//迴圈結束後才存入customer物件，存的是扣完後的新點數
		customer.setCurrentPoints(remainingCurrentPoints);
		customerRepository.save(customer);		
		/*
		 * 為了讓currentPoints 的值一定跟 available_points 的扣除結果一致
		 * 不會有「兩邊各自計算」的風險
		 * 讓方法回傳扣完後客戶的剩餘點數
		 */
		return remainingCurrentPoints;
	}

}
