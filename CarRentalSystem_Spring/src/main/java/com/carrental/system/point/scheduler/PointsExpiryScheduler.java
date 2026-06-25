package com.carrental.system.point.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.carrental.system.login.entity.CustomerBean;
import com.carrental.system.login.repository.CustomerRepository;
import com.carrental.system.login.service.CustomerService;
import com.carrental.system.point.entity.PointsHistory;
import com.carrental.system.point.enums.ChangeType;
import com.carrental.system.point.repository.PointsHistoryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointsExpiryScheduler {

	private final PointsHistoryRepository pointsHistoryRepository;
	private final CustomerRepository customerRepository;
	private final CustomerService customerService;

	// 【為什麼用 @Scheduled】Spring Boot 內建排程，不需要安裝套件
	//@Scheduled(cron = "0 * * * * ?")→每分鐘測試
	// cron = "0 0 0 * * ?" → 每天凌晨 00:00 執行
	// 格式：秒 分 時 日 月 星期
	@Scheduled(cron = "0 0 0 * * ?")
	@Transactional
	public void expirePoints() {
		LocalDateTime now = LocalDateTime.now();
		log.info("【點數過期排程】開始執行，時間：{}", now);

		// 步驟一：找出所有已過期但還有剩餘點數的紀錄
		List<PointsHistory> expiredList = pointsHistoryRepository.findExpiredWithRemainingPoints(now);

		if (expiredList.isEmpty()) {
			log.info("【點數過期排程】無需處理，結束");
			return;
		}

		for (PointsHistory expired : expiredList) {
			Integer expiredPoints = expired.getAvailablePoints();
			Integer custId = expired.getCustId();

			// 步驟二：把這筆紀錄的 availablePoints 設為 0
			expired.setAvailablePoints(0);
			pointsHistoryRepository.save(expired);

//            // 步驟三：更新 customer 的 currentPoints
//            CustomerBean customer = customerRepository.findById(custId)
//                .orElse(null);
//            if (customer == null) continue;
//
//            int newPoints = customer.getCurrentPoints() - expiredPoints;
//            // 【容易忽略】點數不能變負數
//            customer.setCurrentPoints(Math.max(0, newPoints));
//            customerRepository.save(customer);

			// 重構
			// 步驟三：用 customerService 更新點數
			// 【為什麼用 customerService 而不是直接改 Repository】
			// customerService.updatePoints 同時處理 currentPoints 和 totalAccumulated
			// 直接用 Repository 會漏掉 totalAccumulated 的更新
			customerService.updatePoints(custId, -expiredPoints);

			// 步驟四：新增 EXPIRED 點數異動紀錄
			// 重新查詢取得最新點數（updatePoints 已更新資料庫）
			CustomerBean updatedCustomer = customerRepository.findById(custId)
			    .orElse(null);
			if (updatedCustomer == null) continue;

			PointsHistory expiryRecord = new PointsHistory();
			expiryRecord.setCustId(custId);
			expiryRecord.setChangeType(ChangeType.EXPIRED);
			expiryRecord.setPointsChange(-expiredPoints);
			expiryRecord.setRemainPoints(updatedCustomer.getCurrentPoints());
			expiryRecord.setReferenceId(String.valueOf(expired.getRecordId()));
			expiryRecord.setNotes("點數到期：紀錄 #" + expired.getRecordId());
			expiryRecord.setCreateTime(now);
			expiryRecord.setExpireTime(now);
			expiryRecord.setAvailablePoints(null);
			pointsHistoryRepository.save(expiryRecord);

			log.info("【點數過期排程】custId={} 扣除 {} 點，剩餘 {} 點", custId, expiredPoints, updatedCustomer.getCurrentPoints());
		}

		log.info("【點數過期排程】完成，共處理 {} 筆", expiredList.size());
	}
}