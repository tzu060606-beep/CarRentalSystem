package com.carrental.system.point.service;

import java.math.BigDecimal;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.carrental.system.login.entity.CustomerBean;
import com.carrental.system.login.repository.CustomerRepository;
import com.carrental.system.login.service.CustomerService;
import com.carrental.system.point.entity.PointsHistory;
import com.carrental.system.point.entity.PointsRule;
import com.carrental.system.point.enums.ChangeType;
import com.carrental.system.point.exception.CustomerNotFoundException;
import com.carrental.system.point.repository.PointsHistoryRepository;
import com.carrental.system.point.repository.PointsRuleRepository;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class PointsService {

	private final PointsRuleRepository pointsRuleRepository;
	private final CustomerRepository customerRepository;
	private final PointsHistoryRepository pointsHistoryRepository;
	// 注入F2的OrderService
	//private final OrderService orderService;
	//注入F4的TransferOrderService
	//private final TransferOrderService transferOrderService;
	private final PointsHistoryService pointsHistoryService;
	private final CustomerService customerService;
	
	
	public PointsService(
		    PointsRuleRepository pointsRuleRepository,
		    CustomerRepository customerRepository,
		    PointsHistoryRepository pointsHistoryRepository,
		    PointsHistoryService pointsHistoryService,
		    @Lazy CustomerService customerService
		) {
		    this.pointsRuleRepository = pointsRuleRepository;
		    this.customerRepository = customerRepository;
		    this.pointsHistoryRepository = pointsHistoryRepository;
		    this.pointsHistoryService = pointsHistoryService;
		    this.customerService = customerService;
		}
	

	/**
	 * 【租車、專車完成訂單後，發放點數給客戶】
	 *
	 * 呼叫方：F2（租車結案）、F4（專車結束） 呼叫時機：訂單完成（CLOSED）後
	 * 呼叫方式：pointsService.addPoints(custId, orderId, ChangeType.RENTAL)
	 *
	 * @param custId     客戶 ID
	 * @param orderId    訂單 ID（租車或專車）
	 * @param changeType 點數來源類型，目前支援 ChangeType.RENTAL 和 ChangeType.TRANSFER
	 */

	// =======================【API:addPoints】=======================
	// 【租車、專車獲取訂單點數方法】
	// 呼叫方:F2、F4
	// 被呼叫方: F1:addPoints
	// ==============================================================

	//[重構]-因為循環依賴關係，改成PointsEventService呼叫pointService，參數改為加上接收TotalAmount
	public void addPoints(Integer custId, Integer orderId, BigDecimal totalAmount,ChangeType changeType) {
		// F2租車系統或F4專車系統收到完成訂單指示，F2、F4、呼叫addpoints方法，並傳入訂單id、客戶id

		/// 【驗證0】:存在驗證>>有這筆資料嗎(租車系統有做驗證了，這裡不再次驗證)

		// ==============================================================
		// 【A】根據changeType查詢對應規則，取得換算比例ratio
		// ==============================================================
		// 要如何判斷訂單是來自租車還是專車?
		// [重構完成] F2、F4 改為傳入 ChangeType Enum，不再使用字串

		// 呼叫私有輔助方法getRuleKey()將接收到的changeType參數mapping回rulekey
		String ruleKey = getRuleKey(changeType);
		// 在pointRule表裡找到相對應的RuleKey
		PointsRule targetRule = pointsRuleRepository.findByRuleKey(ruleKey);
		
		/// 【驗證1】:確認點數規則啟用中
		// 規則停用就直接返回不發點數，不用再繼續後面的流程
		if (targetRule.getIsActive() == false) {
			return;
		}

///////////////////////[重構]-有循環依賴問題，整個移到PointEventService///////////////////////////////////
 
//		// ==============================================================
//		// 【B】從訂單id取出整筆訂單資料，拿出總金額
//		// 先判定這是專車的訂單還是租車的訂單
//		// ==============================================================
//		/// 【x驗證】:金額有大於最低贈點金額嗎?租車最低50，專車最低33，沒有就不用繼續
//		// 最低贈點金額的驗證先跳過，交給 ratio 計算自然處理（金額太小算出來是 0 點就不存）。
//		
//		// 先宣告一個存金額用的變數
//		BigDecimal totalAmount;
//
//		// [1] 用if判定是專車還是租車訂單
//		if(changeType == ChangeType.RENTAL) {
//			
//			// 1. 透過id取得F2租車訂單
//			RentalOrderBean order = orderService.getOrder(orderId);
//			/// 【驗證1】custId 跟訂單裡的客戶是同一個人嗎?>>防止用別人訂單刷點
//			if (!order.getCustomer().getCustId().equals(custId)) {
//				throw new RuntimeException("客戶ID與訂單不符，禁止發放點數");
//			}
//			/// 【驗證2】:確認該筆訂單狀態是close已結案，租車訂單已經有了，防禦性驗證
//			// 直接拿字串跟呼叫方的Enum比較
//			if (!"CLOSED".equals(order.getOrderStatus().name())) {
//				throw new RuntimeException("租車訂單尚未結案，不能發點數");
//			}
//			
//			// 2. 呼叫方法取得訂單金額
//			totalAmount = order.getTotalAmount();
//			
//		}else if(changeType == ChangeType.TRANSFER){
//			
//			//1. 透過id取得F4專車訂單
//			TransferOrder order = transferOrderService.findById(orderId);
//			/// 【驗證1】custId 跟訂單裡的客戶是同一個人嗎?>>防止用別人訂單刷點
//			if (!order.getCustId().equals(custId)) {
//				throw new RuntimeException("客戶ID與訂單不符，禁止發放點數");
//			}
//			/// 【驗證2】:確認該筆訂單狀態是close已結案，租車訂單已經有了，防禦性驗證
//			// 直接拿字串跟呼叫方的Enum比較
//			if (!"已完成".equals(order.getStatus())) {
//				throw new RuntimeException("專車訂單尚未結案，不能發點數");
//			}
//			
//			// 2. 呼叫方法取得訂單金額
//			totalAmount = order.getTotalAmount();
//			
//		}else {
//			throw new RuntimeException("不支援的changeType"+changeType);
//		}

		// ==============================================================
		// 【C】計算點數:將總金額根據對應規則計算點數
		// ==============================================================
		
		// FLOOR(totalAmount * ratio)
		// BigDecimal 的乘法
		int points = totalAmount.multiply(targetRule.getRatio()).intValue();
		// intValue() 會直接去掉小數，等同於無條件捨去

		// ==============================================================
		// 【D】更新客戶點數: 呼叫客戶的addpoint api 將點數存進客戶的currentPoints
		// ==============================================================

		/// 【驗證1】存在驗證:點數有存進去客戶資料中嗎?
		// TODO: 待 merge 後換成呼叫 customerService.updatePoints(custId, points)
		customerRepository.findById(custId)
				.orElseThrow(() -> new CustomerNotFoundException("客戶不存在"));

		/// 【驗證2】防重複驗證:這筆訂單有沒有重複發放?
		// [重構]-不再需要字串轉 Enum，改成呼叫方傳入enum
		// ChangeType targetChangeType = ChangeType.valueOf(changeType);// changeType字串轉Enum
		// 若發現重複，在history物件建立前就擋掉
		if (pointsHistoryRepository.existsByReferenceIdAndChangeType(String.valueOf(orderId), changeType)) {
			// ChangeType.valueOf(changeType) 字串轉Enum
			throw new RuntimeException("此訂單已發放過點數");
		}

		// 確認沒問題再更新
		// TODO:下方為暫時方案，直接進入customer表修改資料有風險，確認後須更換為對接方案
		customerService.updatePoints(custId, points);
		// [重構]-改成：更新完之後重新查資料庫，拿到最新值
		CustomerBean updateCustomer = customerRepository.findById(custId)
				.orElseThrow(()->new CustomerNotFoundException("客戶不存在"));

		//[刪除]-同步targetCustomer物件,會導致重複發點
		//targetCustomer.setCurrentPoints(targetCustomer.getCurrentPoints() + points);
		//targetCustomer.setCurrentPoints(targetCustomer.getCurrentPoints());

		// ==============================================================
		// 【E】完成點數取得流程、寫入點數異動紀錄
		// ==============================================================

		/// 【不驗證】:點數紀錄是否確定存入?
		/// 成功 → 程式繼續
		/// 失敗 → Spring 直接拋例外，@Transactional 自動倒帶
		PointsHistory history = new PointsHistory();

		history.setCustId(custId);
		history.setChangeType(changeType);
		history.setPointsChange(points);
		history.setRemainPoints(updateCustomer.getCurrentPoints());
		// 把 傳進來的orderId 轉成字串，存進 pointsHistory表中的referenceId，方法內部自己處理，呼叫方不需要再傳其他的id進來
		// 方法的參數應該只包含呼叫方需要提供的資訊，內部可以推算的值不應該暴露到介面（interface）上
		history.setReferenceId(String.valueOf(orderId));
		// 根據傳進來的字串變更Notes的文字，並顯示訂單編號
		history.setNotes(changeType.getDisplayName() + ":訂單 #" + orderId);

		// 寫入點數異動紀錄
		pointsHistoryService.insertPointsHistory(history);

	}

	// =======================【API:addFixedPoints】===========================
	// API:addFixedPoints
	// 【獲取生日贈點、首租獎勵等固定點數新增方法】
	// TODO:待F1註冊流程完成後改為註冊贈點
	// 呼叫方:F2（首租獎勵）、F1（未來生日贈點或新會員贈點）
	// 被呼叫方:F1:customer>>updatePoints(待確認)
	// 不需要訂單金額，直接從規則表拿固定點數
	// ========================================================================

	public void addFixedPoints(Integer custId,ChangeType changeType) {

		// ============================================================
		// 【A】查詢對應的點數規則，取得固定點數
		// ============================================================

		// 直接用 changeType 當 ruleKey 查規則（不需要 getRuleKey() 轉換）
		// 因為 FIRST_RENTAL 的 changeType 和 ruleKey 名稱相同
		// WELCOME_BONUS、FIRST_RENTAL
		
		PointsRule targetRule = pointsRuleRepository.findByRuleKey(changeType.name());

		/// 【驗證1】啟用驗證:確認規則是否啟用中，停用就靜默 return，不發點數
		if (targetRule.getIsActive() == false) {
			return;
		}

		// 取出 ratio，對固定點數規則來說 ratio 就是點數本身（例如 50.0000 → 50 點）
		BigDecimal ratio = targetRule.getRatio();
		// 拿到規則後建立一個fixedPoints物件
		int fixedPoints = ratio.intValue();// BigDecimal轉為int,捨去小數（固定點數本來就是整數，不會有精度問題）

		// ============================================================
		// 【B】驗證：客戶存在 + 防重複：這個 custId 是否已經拿過這個 changeType 的點數？
		// ============================================================

		/// 【驗證1】存在驗證:確認客戶是否存在
		customerRepository.findById(custId)
				.orElseThrow(() -> new CustomerNotFoundException("客戶不存在"));

		/// 【驗證2】防重複驗證:這筆訂單有沒有重複發放?
		// 注意：FIRST_RENTAL 一輩子只能一次，直接查有沒有紀錄就好
		
		// [重構]-不再需要字串轉 Enum，改成呼叫方傳入enum
		// ChangeType targetChangeType = ChangeType.valueOf(changeType);// changeType字串轉Enum
		
		// 若發現重複，在history物件建立前就擋掉
		// 這裡用 custId + changeType 查，不用 referenceId（因為沒有訂單）
		if (pointsHistoryRepository.existsByCustIdAndChangeType(custId, changeType)) {
			// ChangeType.valueOf(changeType) 字串轉Enum
			throw new RuntimeException("點數已發放過");
		}

		// ============================================================
		// 【C】更新客戶點數
		// ============================================================

		// 呼叫私有輔助方法
		// TODO：暫時方案，直接操作 customer 表，待 F1 提供 updatePoints() 後改呼叫對方方法
		customerService.updatePoints(custId, fixedPoints);
		// 同步 Java 物件的點數值，讓下面 remainPoints 可以拿到更新後的數字
		// （updateCustomerPoints 更新了資料庫，但 targetCustomer 物件還是舊的值）
		
		// [重構]-改成：更新完之後重新查資料庫，拿到最新值
		CustomerBean updateCustomer = customerRepository.findById(custId)
						.orElseThrow(()->new CustomerNotFoundException("客戶不存在"));
		//[刪除]-同步targetCustomer物件,會導致重複發點
		//targetCustomer.setCurrentPoints(targetCustomer.getCurrentPoints() + fixedPoints);

		// ============================================================
		// 【D】寫入點數異動紀錄
		// ============================================================

		PointsHistory history = new PointsHistory();
		history.setCustId(custId); // 設定客戶 ID
		history.setPointsChange(fixedPoints); // 設定本次增加的點數
		history.setChangeType(changeType); // 設定點數來源類型（Enum）
		history.setRemainPoints(updateCustomer.getCurrentPoints());// 設定本次異動後的剩餘點數（已同步過的物件值）
		// 固定點數沒有對應訂單，referenceId 保持 null（不需要 setReferenceId）
		// history.setReferenceId(null);

		// 根據傳進來的字串變更Notes的文字，例如：「新會員首租獎勵」
		history.setNotes(changeType.getDisplayName());

		// 呼叫 Service 存入資料庫
		// insertPointsHistory 內部會自動設定 createTime 和 expireTime（+365天）
		pointsHistoryService.insertPointsHistory(history);
	}
	
	
	
	//////////////////////////////////以下為私有輔助方法////////////////////////////////////////

	// ========================================================================
	// 私有輔助方法1
	// 【changeType（RENTAL）→ ruleKey（RENTAL_POINT）】
	// 呼叫方:pointService內部
	// ========================================================================

	// 私有的輔助方法，只有這個 class 內部使用
	// pointsHistoy的changeType跟pointsRule的ruleKey不同時，將「外部代碼對應內部代碼」，進行映射（Mapping）
	private String getRuleKey(ChangeType changeType) {
		switch (changeType) {
		case RENTAL:
			return "RENTAL_POINT";
		case TRANSFER:
			return "TRANSFER_POINT";
		default:
			return changeType.name();//其他的直接用enum名稱
		}
	}
	// 外部傳進來的值和內部使用的值不一定相同，在 Service 層做轉換是正確的做法，不應該讓外部知道內部的 ruleKey 細節。

	// ========================================================================
	// 私有輔助方法2
	// 【更新客戶點數: 呼叫客戶的api 將點數存進客戶的currentPoints】
	// addPoints 和 addFixedPoints 都先呼叫這個私有方法，F1確認是否修改後只需要改一個方法
	// 呼叫方:pointService內部
	// ========================================================================
	// TODO:下方為暫時方案，直接進入customer表修改資料有風險，確認後須更換為對接方案
	
	/////////////////////////對接customerService.updatePoints////////////////////////////////
	
//	private void updateCustomerPoints(Integer custId, int points) {
//		CustomerBean targetCustomer = customerRepository.findById(custId)
//				.orElseThrow(() -> new CustomerNotFoundException("客戶不存在"));
//		
//		// 更新目前點數
//		targetCustomer.setCurrentPoints(targetCustomer.getCurrentPoints() + points);
//		// 只有加點才累積 totalAccumulated，扣點不影響（跟 F1 的 updatePoints 邏輯一致）
//		targetCustomer.setTotalAccumulated(targetCustomer.getTotalAccumulated() + Math.max(0,points));
//		customerRepository.save(targetCustomer);
//	}
}
