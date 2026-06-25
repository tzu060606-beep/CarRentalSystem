package com.carrental.system.point.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.carrental.system.login.entity.CustomerBean;
import com.carrental.system.login.repository.CustomerRepository; // 使用組長正確拼寫的版本
import com.carrental.system.point.dto.RedemptionOrderRequestDTO;
import com.carrental.system.point.dto.RedemptionOrderResponseDTO;
import com.carrental.system.point.dto.RedemptionOrderStatusDTO;
import com.carrental.system.point.dto.VoucherSummaryDTO;
import com.carrental.system.point.entity.PointsHistory;
import com.carrental.system.point.entity.Product;
import com.carrental.system.point.entity.RedemptionOrder;
import com.carrental.system.point.entity.Voucher;
import com.carrental.system.point.enums.ChangeType;
import com.carrental.system.point.enums.OrderStatus;
import com.carrental.system.point.exception.CustomerNotFoundException;
import com.carrental.system.point.exception.InsufficientPointsException;
import com.carrental.system.point.exception.InsufficientStockException;
import com.carrental.system.point.exception.ProductNotActiveException;
import com.carrental.system.point.exception.ProductNotFoundException;
import com.carrental.system.point.exception.RedemptionOrderNotFoundException;
import com.carrental.system.point.repository.ProductRepository;
import com.carrental.system.point.repository.RedemptionOrderRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RedemptionOrderService {

	// 建構子注入
	private final RedemptionOrderRepository redemptionOrderRepository;//存進資料庫用
	private final ProductRepository productRepository;// 扣庫存用
	private final CustomerRepository customerRepository;// 扣點數用
	//private final PointsHistoryRepository pointsHistoryRepository;//新增點數異動紀錄用
	private final PointsHistoryService pointsHistoryService;//新增點數異動紀錄改用Service，因為多了計算時間的欄位
	private final VoucherService voucherService;//新增商品兌換券用、查訂單附帶票券資料用

	// 新增兌換訂單
	// 執行順序:
	// 1. 先建立 RedemptionOrder → 存進資料庫 → 資料庫產生 redemptionId。
	// 2. 拿到 redemptionId 之後 → 才能建立 PointsHistory，把 redemptionId 存進 referenceId
	// 3. 建立好pointsHistory → 新增商品voucher  → 回傳新建立的訂單
	public RedemptionOrder insertRedemptionOrder(Integer custId,RedemptionOrderRequestDTO redemptionOrderRequestDTO) {

		/*
		 * ==========================================================
		 * 【A】查詢商品、客戶
		 * 查詢： 從DTO取得資料，查詢商品和客戶 - 第一層:存在驗證
		 * [你要的東西存在在DB嗎?(本質上還是業務邏輯的一部分)]
		 * ==========================================================
		 */
		// 1. 用 dto.getProductId() 查詢商品
		// 用 productRepository.findById() 查
		// 如果找不到，throw new RuntimeException("商品不存在")
		Integer productId = redemptionOrderRequestDTO.getProductId();
		Optional<Product> targetProduct = productRepository.findById(productId);
		if (targetProduct.isEmpty()) {
			// 為什麼不用NullPointerException，因為這不是bug是正常的業務流程，使用者傳了一個不存在的id
			// NullPointerException → Java 幫你丟，代表程式寫錯了
			// RuntimeException → 你自己丟，代表業務邏輯上出了問題
			// throw new RuntimeException("商品不存在");
			throw new ProductNotFoundException("商品不存在");
		}

		// 2. 用 dto.getCustId() 查詢客戶
		// 用 customerRepository.findById() 查
		// 如果找不到，throw new RuntimeException("客戶不存在")
		//Integer custId = redemptionOrderRequestDTO.getCustId();
		// 改成直接用傳進來的custId查客戶
		Optional<CustomerBean> targetCustomer = customerRepository.findById(custId);
		if (targetCustomer.isEmpty()) {
			//throw new RuntimeException("客戶不存在");
			throw new CustomerNotFoundException("客戶不存在");
		}

		// 把已經抓到商品跟客戶id的dto放進redemptionOrder物件
		// 先把 Optional 取出物件，再一個一個 set 進去
		Product product = targetProduct.get();
		CustomerBean customer = targetCustomer.get();

		/*
		 * ========================================================== 
		 * 【B】驗證上架、庫存、點數
		 * 驗證: 3個前置驗證 - 第二層:業務驗證
		 * [你要的東西有賣嗎?夠賣嗎?你夠錢買嗎?]
		 * ==========================================================
		 */

		// 計算需要花費的點數：pointsSpent = product.getPointsRequired() ×
		// redemptionOrderRequestDTO.getProductQuantity()，然後驗證
		// customer.getCurrentPoints() 是否足夠

		// 驗證一：商品必須上架中
		// 如果 product.getIsActive() 是 false → throw new RuntimeException("商品已下架")
		if (product.getIsActive() == false) {
			//throw new RuntimeException("商品已下架");
			throw new ProductNotActiveException("商品已下架");
		}

		// 驗證二：庫存必須足夠
		// 如果 product.getStockQuantity()
		// 小於redemptionOrderRequestDTO.getProductQuantity() → throw new
		// RuntimeException("庫存不足")
		Integer productQuantity = redemptionOrderRequestDTO.getProductQuantity();
		if (product.getStockQuantity() < productQuantity) {
			//throw new RuntimeException("庫存不足");
			throw new InsufficientStockException("庫存不足");
		}

		// 驗證三：計算 pointsSpent，確認客戶點數是否足夠
		// 存成變數等下還要用
		Integer pointsSpent = product.getPointsRequired() * productQuantity;
		if (customer.getCurrentPoints() < pointsSpent) {
			//throw new RuntimeException("點數不足");
			throw new InsufficientPointsException("點數不足");
		}

		
		
		/*
		 * ========================================================== 
		 * 【C】建立訂單物件
		 * [所有新增的資訊裝進redemptionOrder物件]
		 * ==========================================================
		 */
		
		// 步驟四: 把訂單物件建立好
		// 把從dto拿到的值set到redemptionOrder物件
		RedemptionOrder redemptionOrder = new RedemptionOrder();
		// setCustomerBean 和 setProduct 是在設定訂單要關聯哪個客戶和商品
		redemptionOrder.setCustomerBean(customer);
		redemptionOrder.setProduct(product);
		redemptionOrder.setProductQuantity(productQuantity);
		
		redemptionOrder.setPointsSpent(pointsSpent);//強制由後端計算金額
		/// 訂單狀態使用 OrderStatus Enum：ACTIVE / USED / CANCELLED / EXPIRED
		// @Enumerated(EnumType.STRING) 讓 JPA 存英文字串而不是數字
		redemptionOrder.setOrderStatus(OrderStatus.ACTIVE);// 預設訂單就是待使用
		
		// LocaldayTime設成變數，因為ExpireTime+365要用，分開計算會有極細微的差距
		LocalDateTime now = LocalDateTime.now();
		// CreateTime 只在新增訂單時設定一次，之後永遠不變。
		redemptionOrder.setCreateTime(now);
		
		
		/*
		 * ========================================================== 
		 * 【D】扣庫存、扣點數
		 * 執行: 扣庫存、扣點數 - 第三層:業務邏輯
		 * [訂單下訂同時，庫存跟點數都跟著異動]
		 * ==========================================================
		 */
		
		//扣庫存(現有庫存 - 兌換數量)，然後存回DB
		product.setStockQuantity(product.getStockQuantity()-productQuantity);
		productRepository.save(product);
		
		//扣點數(現有點數 - pointsSpent)，然後存回DB
		//customer.setCurrentPoints(customer.getCurrentPoints()-pointsSpent);
		//customerRepository.save(customer);
		
		//[重構]-改為FIFO扣點，同時更新 customer.currentPoints 並回傳扣完後的剩餘點數
		//先拿到要扣的點數，傳入FIFO扣點方法
		Integer remainingCurrentPoints = pointsHistoryService.deductPointsByFIFO(custId, pointsSpent);
		
		
		
	   /*
	    * ==========================================================
	    * 【E】save訂單，取得資料庫產生的 redemptionId
	    * 必須在建立 PointsHistory 之前，因為 referenceId 需要 redemptionId
	    * ==========================================================
		*/
		
		//先save訂單，才能存進資料庫，才能拿到redemptionId
		RedemptionOrder newRedemptionOrder = redemptionOrderRepository.save(redemptionOrder);

		
		
		/*
		 * ========================================================== 
		 * 【F】寫入點數異動紀錄
		 * 建立點數異動紀錄 - 第四層:業務邏輯
		 * [紀錄這次兌換的點數扣除明細]
		 * ==========================================================
		 */
		
		PointsHistory pointsHistory = new PointsHistory(); 
		pointsHistory.setCustId(custId);
		pointsHistory.setRemainPoints(remainingCurrentPoints);//把扣完後的點數存進PointHistory
		pointsHistory.setChangeType(ChangeType.REDEMPTION);
		//Q: pointSpent要加上正負號嗎?還是前端顯示有加就好，這邊只是單純存異動的數量
		//A: 要加，資料庫就是存有加的，要統一
		pointsHistory.setPointsChange(-pointsSpent);

		//把拿到的redemptionId轉字串存入PointHistory
		pointsHistory.setReferenceId(String.valueOf(newRedemptionOrder.getRedemptionId()));
		pointsHistory.setNotes("兌換: "+product.getProductName()+"x"+productQuantity+"件");// 備註帶入商品名稱+數量，方便查詢
		
		//[移除]-這邊移除，讓PointHistoryService新增時自己set		
		//pointsHistory.setCreateTime(now);
		//pointsHistory.setExpireTime(now.plusDays(365));// 點數有效期限：一年後到期
		
		//[重構]呼叫PointsHistory裡面的新增方法-->新增一筆點數異動紀錄，並同時設定 createTime 和 expireTime 
		pointsHistoryService.insertPointsHistory(pointsHistory);
		
		/*
		 * ========================================================== 
		 * 【G】產生 voucher
		 * - 第五層:業務邏輯
		 * [訂單完成後新增商品兌換券]
		 * ==========================================================
		 */
		// insertVoucher 需要 newRedemptionOrder（有 redemptionId 的版本）
		// 不是還沒存入資料庫的 redemptionOrder。
		// insertVoucher(newRedemptionOrder) 傳入已有 redemptionId 的訂單
		voucherService.insertVoucher(newRedemptionOrder);
		
		
		// 回傳新建立的訂單
		return newRedemptionOrder;
	}

	
	
	
	
	// 修改兌換訂單狀態 
	//這裡只更新修改訂單狀態，只需要傳狀態，不需要傳整個訂單物件。建DTO
	public RedemptionOrder updateRedemptionOrderStatus(Integer id, RedemptionOrderStatusDTO redemptionOrderStatusDTO) {
		Optional<RedemptionOrder> targetRedemptionOrder = redemptionOrderRepository.findById(id);
		if (targetRedemptionOrder.isEmpty()) {
			//throw new RuntimeException("查無此筆訂單");
			throw new RedemptionOrderNotFoundException("查無此筆訂單");
		} else {
			RedemptionOrder r = targetRedemptionOrder.get();

			//	訂單的這些客戶資訊、商品資訊、商品數量、花費點數、建立時間等資訊應不可被更改
			//	r.setCustomerBean(redemptionOrder.getCustomerBean());
			//	r.setProduct(redemptionOrder.getProduct());
			//	r.setProductQuantity(redemptionOrder.getProductQuantity());
			//	r.setPointsSpent(redemptionOrder.getPointsSpent());
			//	r.setCreateTime(redemptionOrder.getCreateTime());

			// 只有在狀態真的改變時才更新[更新時間]欄位
			// Q:為什麼不用equals比較?
			// A:equals 是比較內容，!= 是比較記憶體位置。
			// A:Enum 是單例，每個值在 JVM 裡只有一個實例，OrderStatus.ACTIVE 永遠指向同一個記憶體位置，所以可以直接用 != 比較
			if (r.getOrderStatus()!=(redemptionOrderStatusDTO.getOrderStatus())) {
				r.setOrderStatus(redemptionOrderStatusDTO.getOrderStatus());
				// UpdateTime 每次狀態改變時更新
				r.setUpdateTime(LocalDateTime.now());
			}

			RedemptionOrder updateRedemptionOrder = redemptionOrderRepository.save(r);
			return updateRedemptionOrder;
		}
	}

	// 刪除兌換訂單
	public void deleteRedemptionOrderById(Integer id) {
		redemptionOrderRepository.findById(id)
		 .orElseThrow(() -> new RedemptionOrderNotFoundException("查無此筆訂單"));
			redemptionOrderRepository.deleteById(id);
	}

	// 查詢單筆兌換訂單
	public RedemptionOrder findRedemptionOrderById(Integer id) {
		Optional<RedemptionOrder> targetRedemptionOrder = redemptionOrderRepository.findById(id);
		//return targetRedemptionOrder.orElse(null);改成拋例外
		//orElseThrow 的語法需要傳入一個供應者（Supplier），不是直接傳例外物件。
		//() -> 的意思是「當需要的時候才建立這個例外」，這是 Java 的 Lambda 語法。
		return targetRedemptionOrder.orElseThrow(()->new RedemptionOrderNotFoundException("查無此筆訂單"));
	}

	// 查詢全部兌換訂單
	public List<RedemptionOrder> findAllRedemptionOrders() {
		List<RedemptionOrder> redemptionOrdersList = redemptionOrderRepository.findAll();
		return redemptionOrdersList;
	}

	// 查詢兌換訂單關鍵字
	public List<RedemptionOrder> findRedemptionOrdersByKeyword(String keyword) {
		List<RedemptionOrder> redemptionOrdersList = redemptionOrderRepository.findByKeyword(keyword);
		return redemptionOrdersList;
	}

	// 依訂單狀態篩選兌換訂單
	public List<RedemptionOrder> findRedemptionOrdersByOrderStatus(String orderStatus) {
		List<RedemptionOrder> redemptionOrdersList = redemptionOrderRepository.findByOrderStatus(orderStatus);
		return redemptionOrdersList;
	}
	
	//  查全部訂單，並附帶票券資訊
	//→ 對每筆訂單，附上這筆訂單的票券摘要
	//→ 組成 List<RedemptionOrderResponseDTO> 回傳
	public List<RedemptionOrderResponseDTO> findAllOrdersWithVouchers() {
	    
		// 【A】：查全部訂單（JOIN FETCH 帶出客戶和商品，避免 N+1）
	    List<RedemptionOrder> redemptionOrdersList = redemptionOrderRepository.findAllWithDetails();
	    
	    // 【B】：準備一個空的 List 裝結果
	    List<RedemptionOrderResponseDTO> result = new ArrayList<>();
	    
	    // 【C】：對每筆訂單做處理
	    for (RedemptionOrder order : redemptionOrdersList) {
	        
	        // 1. 用 redemptionId 查這筆訂單的所有票券
	        // 【為什麼用 voucherService】不直接用 Repository，
	        // 讓 Service 層管理業務邏輯，符合分層架構
			List<Voucher> voucherList = voucherService.findByRedemptionId(order.getRedemptionId());

	        
			 // 2. 把每張 Voucher 轉成 VoucherSummaryDTO
	        // 【為什麼要轉】不回傳整個 Voucher 物件，只回傳後台需要的欄位
			 List<VoucherSummaryDTO> voucherSummaries = new ArrayList<>();
			 for (Voucher voucher : voucherList) {
				 VoucherSummaryDTO voucherSummaryDTO = new VoucherSummaryDTO();
				 voucherSummaryDTO.setVoucherCode(voucher.getVoucherCode());
				 voucherSummaryDTO.setStatus(voucher.getStatus().name()); // Enum 轉字串
				 voucherSummaryDTO.setExpiryDate(voucher.getExpiryDate() != null
		                ? voucher.getExpiryDate().toString() : null);
		         voucherSummaries.add(voucherSummaryDTO);
			}

	        // 3. 組成 RedemptionOrderResponseDTO
			RedemptionOrderResponseDTO redemptionOrderResponseDTO = new RedemptionOrderResponseDTO();
			redemptionOrderResponseDTO.setRedemptionId(order.getRedemptionId());
			redemptionOrderResponseDTO.setCustId(order.getCustomerBean().getCustId());
			redemptionOrderResponseDTO.setCustName(order.getCustomerBean().getCustName());
			redemptionOrderResponseDTO.setProductId(order.getProduct().getProductId());
			redemptionOrderResponseDTO.setProductName(order.getProduct().getProductName());
			redemptionOrderResponseDTO.setProductQuantity(order.getProductQuantity());
			redemptionOrderResponseDTO.setPointsSpent(order.getPointsSpent());
			redemptionOrderResponseDTO.setOrderStatus(order.getOrderStatus());
			redemptionOrderResponseDTO.setCreateTime(order.getCreateTime());
			// 【核心】把票券摘要列表塞進 DTO
			redemptionOrderResponseDTO.setVouchers(voucherSummaries); // 票券摘要列表
	        
	        
	        // 4. 加進結果 List
	        result.add(redemptionOrderResponseDTO);
	    }
	    
		return result;
	}
	
	
}
