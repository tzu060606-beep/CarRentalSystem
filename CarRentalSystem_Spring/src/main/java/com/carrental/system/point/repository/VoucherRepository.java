package com.carrental.system.point.repository;

import com.carrental.system.point.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

	// 【根據 redemptionId 查詢所有 voucher】
	// 傳兌換訂單id，穿透RedemptionOrder-->找到RedemptionId
	List<Voucher> findByRedemptionOrderRedemptionId(Integer redemptionId);

	// 【根據 custId 查詢所有 voucher】
	// 傳客戶id，穿透RedemptionOrder-->穿透CustomerBean-->找到CustomerId
	// 後端用 custId 查，前端顯示名稱。原因是名稱可能重複（兩個客戶叫同一個名字），用 id 查才精確。
	List<Voucher> findByRedemptionOrderCustomerBeanCustId(Integer custId);

	// 【根據 voucherCode 查詢單筆 voucher】
	// voucherCode 是 UNIQUE，一個 code 只會對應一筆-->回傳型別為Optional
	Optional<Voucher> findByVoucherCode(String voucherCode);

}