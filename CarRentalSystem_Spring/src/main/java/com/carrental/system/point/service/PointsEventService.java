package com.carrental.system.point.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.carrental.system.point.enums.ChangeType;
import com.carrental.system.rentalorder.entity.RentalOrderBean;
import com.carrental.system.rentalorder.service.OrderService;
import com.carrental.system.transfer.entity.TransferOrder;
import com.carrental.system.transfer.service.TransferOrderService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class PointsEventService {
	
	//先注入所有需要的Service
	private final PointsService pointsService;
	private final OrderService  orderService;
	private final  TransferOrderService transferOrderService;
	
	public void addPoints(Integer custId, Integer orderId,ChangeType changeType) {
		
		
		// ==============================================================
		// 【B】從訂單id取出整筆訂單資料，拿出總金額
		// 先判定這是專車的訂單還是租車的訂單
		// ==============================================================
		/// 【x驗證】:金額有大於最低贈點金額嗎?租車最低50，專車最低33，沒有就不用繼續
		// 最低贈點金額的驗證先跳過，交給 ratio 計算自然處理（金額太小算出來是 0 點就不存）。
		
		// 先宣告一個存金額用的變數
		BigDecimal totalAmount;

		// [1] 用if判定是專車還是租車訂單
		if(changeType == ChangeType.RENTAL) {
			
			// 1. 透過id取得F2租車訂單
			RentalOrderBean order = orderService.getOrder(orderId);
			/// 【驗證1】custId 跟訂單裡的客戶是同一個人嗎?>>防止用別人訂單刷點
			if (!order.getCustomer().getCustId().equals(custId)) {
				throw new RuntimeException("客戶ID與訂單不符，禁止發放點數");
			}
			/// 【驗證2】:確認該筆訂單狀態是close已結案，租車訂單已經有了，防禦性驗證
			// 直接拿字串跟呼叫方的Enum比較
			if (!"CLOSED".equals(order.getOrderStatus().name())) {
				throw new RuntimeException("租車訂單尚未結案，不能發點數");
			}
			
			// 2. 呼叫方法取得訂單金額
			totalAmount = order.getTotalAmount();
			System.out.println("租車訂單總金額為:"+totalAmount);
			
		}else if(changeType == ChangeType.TRANSFER){
			
			//1. 透過id取得F4專車訂單
			TransferOrder order = transferOrderService.findById(orderId);
			/// 【驗證1】custId 跟訂單裡的客戶是同一個人嗎?>>防止用別人訂單刷點
			if (!order.getCustId().equals(custId)) {
				throw new RuntimeException("客戶ID與訂單不符，禁止發放點數");
			}
			/// 【驗證2】:確認該筆專車訂單狀態是已完成
			if (!"已完成".equals(order.getStatus())) {
				throw new RuntimeException("專車訂單尚未結案，不能發點數");
			}
			
			// 2. 呼叫方法取得訂單金額
			totalAmount = order.getTotalAmount();
			
		}else {
			throw new RuntimeException("不支援的changeType"+changeType);
		}
		
		//呼叫 pointService 執行方法
		pointsService.addPoints(custId, orderId, totalAmount, changeType);
	}
	
	public void addFixedPoints(Integer custId,ChangeType changeType) {
		pointsService.addFixedPoints(custId, changeType);
	}
	

}
