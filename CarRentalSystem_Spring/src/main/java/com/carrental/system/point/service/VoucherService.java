package com.carrental.system.point.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.carrental.system.point.dto.VoucherResponseDTO;
import com.carrental.system.point.entity.RedemptionOrder;
import com.carrental.system.point.entity.Voucher;
import com.carrental.system.point.enums.OrderStatus;
import com.carrental.system.point.enums.VoucherStatus;
import com.carrental.system.point.exception.VoucherAlreadyUsedException;
import com.carrental.system.point.exception.VoucherExpiredException;
import com.carrental.system.point.exception.VoucherNotFoundException;
import com.carrental.system.point.repository.RedemptionOrderRepository;
import com.carrental.system.point.repository.VoucherRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class VoucherService {

	private final VoucherRepository voucherRepository;// 存Voucher用
	private final RedemptionOrderRepository redemptionOrderRepository;// 更新兌換訂單狀態用

	// ================================================================
	// 【新增兌換券】
	// 兌換訂單建立時自動呼叫
	// ================================================================
	public Integer insertVoucher(RedemptionOrder redemptionOrder) {
		// 一筆訂單產生複數張兌換券
		// 【A】 從 redemptionOrder 取得 productQuantity（要產生幾張）
		Integer voucherQuantity = redemptionOrder.getProductQuantity();

		// 【B】 迴圈 productQuantity 次：產生voucher
		for (int i = 0; i < voucherQuantity; i++) {
			Voucher voucher = new Voucher();

			// 【C】set各個欄位
			// - 設定訂單
			voucher.setRedemptionOrder(redemptionOrder);
			// - 產生唯一的 voucherCode（UUID）
			//voucher.setVoucherCode(UUID.randomUUID().toString());
			//[重構]-調整生成格式-後面 8 碼來自 UUID
			String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
			voucher.setVoucherCode("VC-" + uuid);
			// - 設定 status = UNUSED
			voucher.setStatus(VoucherStatus.UNUSED);// 預設就是UNUSED
			// - 設定 expiryDate = redemptionOrder.createTime + 365天
			voucher.setExpiryDate(redemptionOrder.getCreateTime().plusDays(365));
			// 未使用，use_time 為 null(不設定預設null)

			// 【D】 存入voucher資料庫
			voucherRepository.save(voucher);
		}
		// 【E】 回傳產生的張數
		return voucherQuantity;
	}

	// ================================================================
	// 【使用兌換券】
	// 算是一種更新狀態的操作
	// 客戶核銷時呼叫
	// ================================================================
	public VoucherResponseDTO useVoucher(String voucherCode) {

		// 【A】用 voucherCode 查 voucher
		// 找不到就拋runtimeException例外
		Optional<Voucher> voucher = voucherRepository.findByVoucherCode(voucherCode);
		Voucher targetVoucher = voucher.orElseThrow(() -> new VoucherNotFoundException("找不到此筆兌換券"));

		// [刪除]- targetRedemptionOrder從voucher直接取出物件，不可能是null（JPA 關聯會自動載入）。不需要另外驗證。
		// 確認RedemptionOrder是否存在
		// RedemptionOrder targetRedemptionOrder = targetVoucher.getRedemptionOrder();
		// Integer targetRedemptionId = targetRedemptionOrder.getRedemptionId();
		// if(targetRedemptionOrder) {
		// return new RuntimeException("找不到此筆訂單");
		// }

		// 【B】確認沒有過期
		// 用isBefore()比較 voucher.getExpiryDate() 和現在時間
		LocalDateTime expiryDate = targetVoucher.getExpiryDate();
		if (expiryDate.isBefore(LocalDateTime.now())) {
			throw new VoucherExpiredException("此兌換券已過期");
		}

		// 【C】確認狀態是 UNUSED
		// 已使用或已過期都要擋掉
		VoucherStatus targetVoucherStatus = targetVoucher.getStatus();
		if (targetVoucherStatus != VoucherStatus.UNUSED) {
			// VoucherStatus 是 class 名稱，用它來取常數
			throw new VoucherAlreadyUsedException("此兌換券已使用");
		}

		// 【D】更新 voucher 狀態和使用時間
		VoucherStatus updatedStatus = VoucherStatus.USED;
		targetVoucher.setStatus(updatedStatus);
		targetVoucher.setUseTime(LocalDateTime.now());

		// 【E】更新 redemptionOrder 狀態為 USED
		// 建立一個訂單物件
		RedemptionOrder targetRedemptionOrder = targetVoucher.getRedemptionOrder();
		// 設定訂單狀態
		targetRedemptionOrder.setOrderStatus(OrderStatus.USED);
		// 把訂單物件整筆存入兌換訂單表
		redemptionOrderRepository.save(targetRedemptionOrder);

		// 【F】存入資料庫
		voucherRepository.save(targetVoucher);

		// 【G】組裝 VoucherResponseDTO 回傳
		VoucherResponseDTO voucherResponseDTO = new VoucherResponseDTO();
		String productName = targetVoucher.getRedemptionOrder().getProduct().getProductName();
		voucherResponseDTO.setVoucherCode(voucherCode);
		voucherResponseDTO.setStatus(updatedStatus);
		voucherResponseDTO.setExpiryDate(expiryDate);
		voucherResponseDTO.setProductName(productName);
		return voucherResponseDTO;
	}

	// ================================================================
	// 【查詢單筆兌換券By voucherCode】
	// 根據voucherCode查特定單筆訂單
	// ================================================================
	public Voucher findByVoucherCode(String voucherCode) {
		Optional<Voucher> targetVoucher = voucherRepository.findByVoucherCode(voucherCode);
		return targetVoucher.orElseThrow(() -> new VoucherNotFoundException("找不到此筆兌換券"));
	}

	// ================================================================
	// 【查詢所有兌換券By RedemptionId】
	// 查某筆訂單的所有 voucher
	// ================================================================
	public List<Voucher> findByRedemptionId(Integer redemptionId) {
		List<Voucher> voucherList = voucherRepository.findByRedemptionOrderRedemptionId(redemptionId);
		return voucherList;
	}

	// ================================================================
	// 【查詢所有兌換券By CustId】
	// 查某位客戶的所有 voucher
	// ================================================================
	public List<Voucher> findByCustId(Integer custId) {
		List<Voucher> voucherList = voucherRepository.findByRedemptionOrderCustomerBeanCustId(custId);
		return voucherList;
	}
	
	
	// ================================================================
	// 【查全部兌換券】
	// ================================================================
	public List<Voucher> findAllVouchers() {
		List<Voucher> vouchersList = voucherRepository.findAll();
		return vouchersList;
	}

	// 刪除：voucher 不應該被刪除，它是核銷紀錄，刪掉會失去追蹤能力。
	// 就算訂單取消，voucher 也應該改狀態（EXPIRED）而不是刪除。

}
