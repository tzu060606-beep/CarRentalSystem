package com.carrental.system.rentalorder.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carrental.system.login.repository.CustomerRepository;
import com.carrental.system.point.enums.ChangeType;
import com.carrental.system.point.service.PointsEventService;
import com.carrental.system.point.service.PointsHistoryService;
import com.carrental.system.rentalorder.dto.AdminLongTermReqDto;
import com.carrental.system.rentalorder.dto.AdminLongTermUpdateDto;

import com.carrental.system.rentalorder.dto.AdminOrderReqDto;
import com.carrental.system.rentalorder.dto.AdminOrderUpdateReqDto;
import com.carrental.system.rentalorder.dto.AdminPlanReqDto;
import com.carrental.system.rentalorder.dto.AdminShortTermReqDto;
import com.carrental.system.rentalorder.dto.AdminShortTermUpdateDto;
import com.carrental.system.rentalorder.dto.CustomerOrderReqDto;
import com.carrental.system.rentalorder.dto.MultiInsertRequest;
import com.carrental.system.rentalorder.entity.LongTermDetailBean;
import com.carrental.system.rentalorder.entity.RentalOrderBean;
import com.carrental.system.rentalorder.entity.RentalPlansBean;
import com.carrental.system.rentalorder.entity.ShortTermDetailBean;
import com.carrental.system.rentalorder.enums.FuelLevel;
import com.carrental.system.rentalorder.enums.OrderStatus;
import com.carrental.system.rentalorder.enums.OrderType;
import com.carrental.system.rentalorder.enums.PayStatus;
import com.carrental.system.rentalorder.enums.PaymentMethod;
import com.carrental.system.rentalorder.repository.LongTermDetailRepository;
import com.carrental.system.rentalorder.repository.RentalOrderRepository;
import com.carrental.system.rentalorder.repository.RentalPlansBeanRepository;
import com.carrental.system.rentalorder.repository.ShortTermDetailRepository;
import com.carrental.system.vehicle.controller.VehicleController;
import com.carrental.system.vehicle.entity.Location;
import com.carrental.system.vehicle.entity.Vehicle;
import com.carrental.system.vehicle.entity.VehicleStatus;
import com.carrental.system.vehicle.repository.LocationRepository;
import com.carrental.system.vehicle.repository.VehicleRepository;
import com.carrental.system.vehicle.service.CrossLocationFeeService;
import com.carrental.system.vehicle.service.VehicleService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;

import lombok.Data;

@Service
public class OrderService {
	
	@Autowired
	private RentalOrderRepository orderRepos;
	
	@Autowired
	private ShortTermDetailRepository shortTermRepos;
	
	@Autowired
	private LongTermDetailRepository longTermRepos;
	
	@Autowired
	private RentalPlansBeanRepository plansRepos;

	@Autowired
	private VehicleRepository vehicleRepo;

	@Autowired
	private LocationRepository locationRepo;

	@Autowired
    private VehicleService vehicleService;

	@Autowired
	private CustomerRepository customerRepo; // 假設有這個

	

	@Autowired
	@Lazy
	private PointsEventService pointsEventService;

	@Autowired
    private CrossLocationFeeService locationFeeService;


// ==========================================
// 查詢單筆方法
// ==========================================	
	@Transactional(readOnly = true)
	public RentalOrderBean getOrder(int id) {
		
		//先取得Optional<RentalOrderBean>，拿到箱子但未拆箱
		//另外有寫法為return repository.findById(id).orElseThrow(() -> new RuntimeException("找不到資料"));
		//也可以寫return repository.findById(id).orElse(null); 沒東西就是null
		Optional<RentalOrderBean> optional = orderRepos.findById(id);
		if(optional.isPresent()) {
			return optional.get();
		}else {
			throw new RuntimeException("找不到資料");
		}
	}
	
// ==========================================
// 查詢全部方法
// ==========================================
	@Transactional(readOnly = true)
	public List<RentalOrderBean> findAllOrder(){
		return orderRepos.findAll();
	}
	
//----------------新增(雖然跟更新一樣，但我先分開)--------------

	// @Transactional
	// public RentalOrderBean insert(RentalOrderBean RentalOrder) {
		
	// 	return orderRepos.save(RentalOrder);
	
	// }


// ==========================================
// @新增方法
// ==========================================

	// ==========================================
    // 前台客人新增後傳給後台管理者方法(共用驗證)
    // ==========================================

		@Transactional
    	public RentalOrderBean customerInsert(CustomerOrderReqDto custReq) {
        
        	// 1. 服務生把客人的單子，轉換成主廚看得懂的 MultiInsertRequest
        	AdminOrderReqDto adminOrderDto = new AdminOrderReqDto();
			BeanUtils.copyProperties(custReq, adminOrderDto);
			adminOrderDto.setPayStatus(PayStatus.UNPAID); // 服務生強制鎖死：未付款
        
        	// 🌟 2. 服務生幫客人準備「空的短租明細盒」，避免service報錯！
        	if (adminOrderDto.getOrderType() == OrderType.SHORT_TERM) {

       			adminOrderDto.setShortTerm(new AdminShortTermReqDto()); 
    		}
        
        	// 3. 服務生把整理好的單子，交給service！
        	return this.insert(adminOrderDto); 
   		}


	// ==========================================
    // 後台管理者驗證
    // ==========================================

	@Transactional
	public RentalOrderBean insert(AdminOrderReqDto rentalOrderDto) {
		
		RentalOrderBean rentalOrder = new RentalOrderBean();

		
		AdminShortTermReqDto shortTermReqDto = rentalOrderDto.getShortTerm();
		AdminLongTermReqDto longTermReqDto = rentalOrderDto.getLongTerm();
		

		
	    // 零、 跨模組防呆驗證 (F1, F6)
	    
		
	    // 1. 檢查車輛狀態 (呼叫 F6 車輛 Service，確保車輛是 AVAILABLE)
	    // vehicleService.checkAvailable(rentalOrder.getVehicleId());
		//這個方法要直接從controller呼叫 vehicleService.getAvailableVehicles(rentalOrderDto.getPickupTime(),rentalOrderDto.getReturnTime());
		vehicleService.checkAvailableForBooking(rentalOrderDto.getVehicleId(),rentalOrderDto.getPickupTime(),rentalOrderDto.getReturnTime());
	    
	    // 2. 檢查客戶狀態 (呼叫 F1 會員 Service，確保客戶未被停權)
	    // customerService.checkActive(rentalOrder.getCustId());
		
		
		// 一、RentalOrderBean處理
    	// 0. 找出方案 (防呆)
    	// RentalPlansBean plan = plansRepos.findById(rentalOrderDto.getPlanId())
        //     .orElseThrow(() -> new RuntimeException("系統錯誤：查無此方案ID"));

			RentalPlansBean plan;
			if (rentalOrderDto.getPlanId() != null) {
				// 情境 A：管理員手動指定
				plan = plansRepos.findById(rentalOrderDto.getPlanId())
					.orElseThrow(() -> new RuntimeException("查無此方案ID"));
			} else {
				// 情境 B：客戶端自動配對
				Vehicle vehicle = vehicleRepo.findById(rentalOrderDto.getVehicleId())
					.orElseThrow(() -> new RuntimeException("找不到車輛"));
				String vType = vehicle.getCarModel().getVehicleType();
				boolean isLong = rentalOrderDto.getOrderType() == OrderType.LONG_TERM;
				plan = plansRepos.findByAppliedVehicleTypeAndIsLongTerm(vType, isLong)
					.orElseThrow(() -> new RuntimeException("找不到符合方案：" + vType));
			}


		// 1. 計算租金 (Rental Fee)
		BigDecimal rentalFee = BigDecimal.ZERO;
		BigDecimal calculatedHourtimeFee = BigDecimal.ZERO;
			
		// 🌟 步驟一：先根據「日租/長租」算出系統標準價格，作為防呆比對的基準
		BigDecimal calculatedRentalFee = BigDecimal.ZERO;
		if (rentalOrderDto.getOrderType() == OrderType.SHORT_TERM) {
			calculatedRentalFee = calculateShortTermFee(rentalOrderDto, plan);
			calculatedHourtimeFee = calculateShortTermOvertimeFee(rentalOrderDto, plan);
		} else if (rentalOrderDto.getOrderType() == OrderType.LONG_TERM) {
			calculatedRentalFee = calculateLongTermFee(longTermReqDto, plan);
		}
		//直接在這裡把「天數租金」跟「加時費」加起來，最後準備存在rentalFee欄位
		calculatedRentalFee = calculatedRentalFee.add(calculatedHourtimeFee);

		// 🌟 步驟二：判斷前端傳來的值是否「大於 0」且「不等於系統算出來的標準價格」
		if (rentalOrderDto.getRentalFee() != null && 
			rentalOrderDto.getRentalFee().compareTo(BigDecimal.ZERO) > 0) {
			// 【情境 A：管理者手動覆蓋】(真的有改數字才採用前端的值)
			rentalFee = rentalOrderDto.getRentalFee();
		} else {
			// 【情境 B：系統自動計算租金】(前端沒改，就用系統剛剛算好的)
			rentalFee = calculatedRentalFee;
		}
			
		// ==========================================
			
		// 2. 計算預訂時的額外費用 (目前只有甲租乙還)
		BigDecimal extraFee = BigDecimal.ZERO;

		// 步驟一，F6的甲租乙還方法
		BigDecimal dispatchFee = locationFeeService.getFee(
            rentalOrderDto.getPickupLocationId(), 
            rentalOrderDto.getReturnLocationId()
    	);
		//現在extraFee只裝甲租乙還
		BigDecimal calculatedExtraFee = dispatchFee;

		// 步驟三：防呆比對
		if (rentalOrderDto.getExtraFee() != null && 
			rentalOrderDto.getExtraFee().compareTo(BigDecimal.ZERO)>0) {
			// 【情境 A：管理者手動覆蓋】
			extraFee = rentalOrderDto.getExtraFee();
		} else {
			// 【情境 B：系統自動計算】
			extraFee = calculatedExtraFee;
		
		}


		//  3. 算預估總金額和尾款，最上面有先new 一個rentalOrder
		// 算預估總金額(基礎金額加上預估)
		BigDecimal totalAmount = rentalFee.add(extraFee);
		//
		BigDecimal deposit = totalAmount.multiply(new BigDecimal("0.1"))
                                .setScale(0, RoundingMode.HALF_UP);
								//做四捨五入
		// 算尾款
   	 	BigDecimal remainingBalance = totalAmount.subtract(deposit);
		

		// 4. 後台管理員「狀態」檢驗 (抽成私有方法)

		validatePaymentStatus(rentalOrderDto,deposit);

		// 5. 上面計算完後最底下存進資料庫
		BeanUtils.copyProperties(rentalOrderDto, rentalOrder);
			//把原本Dto的複製進 rentalOrder，接著開始存入上面1、2、3算出來的
		
		// (1) 找客戶資料
        if (rentalOrderDto.getCustId() != null) {
            rentalOrder.setCustomer(customerRepo.findById(rentalOrderDto.getCustId())
                .orElseThrow(() -> new RuntimeException("找不到客戶 ID: " + rentalOrderDto.getCustId())));
        }

		// (2) 找車輛資料
        if (rentalOrderDto.getVehicleId() != null) {
            rentalOrder.setVehicle(vehicleRepo.findById(rentalOrderDto.getVehicleId())
                .orElseThrow(() -> new RuntimeException("找不到車輛 ID: " + rentalOrderDto.getVehicleId())));
        }
		// (3) 找取車據點
        if (rentalOrderDto.getPickupLocationId() != null) {
            rentalOrder.setPickupLocation(locationRepo.findById(rentalOrderDto.getPickupLocationId())
                .orElseThrow(() -> new RuntimeException("找不到取車據點 ID: " + rentalOrderDto.getPickupLocationId())));
        }

		// (4) 找據點
        if (rentalOrderDto.getReturnLocationId() != null) {
            rentalOrder.setReturnLocation(locationRepo.findById(rentalOrderDto.getReturnLocationId())
                .orElseThrow(() -> new RuntimeException("找不到還車據點 ID: " + rentalOrderDto.getReturnLocationId())));
        }


		// 存入
		
		rentalOrder.setRentalFee(rentalFee);
		rentalOrder.setExtraFee(extraFee);
		rentalOrder.setTotalAmount(totalAmount);
		rentalOrder.setDeposit(deposit);
		rentalOrder.setRemainingBalance(remainingBalance); // @Transient 欄位
		
        rentalOrder.setRentalPlan(plan);//找方案最上面就找過了
		//這邊原本要setCostmer等等資料，不過直接寫在上面


		// 6.主子表的存入
		if (rentalOrderDto.getOrderType()==OrderType.SHORT_TERM ) {

			if(shortTermReqDto==null){
				throw new IllegalArgumentException("資料錯誤：建立短租訂單時，必須提供短租詳細資訊");

			}

			if (shortTermReqDto != null) {
    
				// 短租驗證 1：如果有給實際取還車時間，還車不能早於取車
				if (shortTermReqDto.getActualPickupTime() != null && shortTermReqDto.getActualReturnTime() != null) {
					if (shortTermReqDto.getActualReturnTime().isBefore(shortTermReqDto.getActualPickupTime())) {
						throw new IllegalArgumentException("資料錯誤：實際還車時間不能早於實際取車時間！");
					}
				}

				// 短租驗證 2：如果有給里程數，還車里程必須大於等於出發里程
				if (shortTermReqDto.getStartMileage() != null && shortTermReqDto.getEndMileage() != null) {
					if (shortTermReqDto.getEndMileage() < shortTermReqDto.getStartMileage()) {
						throw new IllegalArgumentException("資料錯誤：還車里程數不能小於出發里程數！");
					}
				}
			}


			ShortTermDetailBean shortTerm = new ShortTermDetailBean();
			BeanUtils.copyProperties(shortTermReqDto, shortTerm);
			
			// 雙向綁定：互相認識對方
			shortTerm.setRentalOrder(rentalOrder); 
			rentalOrder.setShortTermDetail(shortTerm); 
        
    	} else if (rentalOrderDto.getOrderType()==OrderType.LONG_TERM) {

			if(longTermReqDto==null){

				throw new IllegalArgumentException("資料錯誤：建立長租訂單時，必須提供長租詳細資訊 (如合約月數)");

			}
		

			LongTermDetailBean longTerm = new LongTermDetailBean();
			BeanUtils.copyProperties(longTermReqDto, longTerm);

			// 1. 處理每月應付金：管理員沒傳，才帶入方案的基礎價格
			if (longTermReqDto.getMonthlyPayment() == null) {
				longTerm.setMonthlyPayment(plan.getBasePrice());
			}
		
			// 2. 處理已付期數：管理員沒傳，新訂單預設為 0
			if (longTermReqDto.getPaidMonths() == null) {
				longTerm.setPaidMonths(0);
			}
		
			// 3. 處理扣款日：管理員沒傳，才自動抓取預計取車時間的「日」
			if (longTermReqDto.getBillingDay() == null && rentalOrderDto.getPickupTime() != null) {
				int dayOfMonth = rentalOrderDto.getPickupTime().getDayOfMonth();
				longTerm.setBillingDay(dayOfMonth);
			}
			
			longTerm.setRentalOrder(rentalOrder);
			rentalOrder.setLongTermDetail(longTerm);
		}

	return orderRepos.save(rentalOrder);
		// 7.連動其他模組
		// 訂單成立後，必須把車輛狀態鎖定，避免被其他人租走
		// vehicle.setStatus("RESERVED");
    	// vehicleRepo.save(vehicle);


	}

// ==========================================
// 私有方法1:日租計算單獨拉出來處理
// ==========================================
	private BigDecimal calculateShortTermFee(AdminOrderReqDto rentalOrderDto, RentalPlansBean plan) {
		//這裡的rentalOrderDto會從主新增傳進來
		if (rentalOrderDto.getPickupTime() == null || rentalOrderDto.getReturnTime() == null) {
        throw new IllegalArgumentException("資料錯誤：短租必須提供取車與還車時間");
    	}

		LocalDateTime pickupTime = rentalOrderDto.getPickupTime();
    	LocalDateTime returnTime = rentalOrderDto.getReturnTime();

		if (returnTime.isBefore(pickupTime) || returnTime.isEqual(pickupTime)) {
        throw new IllegalArgumentException("資料錯誤：還車時間必須晚於取車時間");
    	}

		// 計算總小時數
    	Duration duration = Duration.between(pickupTime, returnTime);
   		long hours = duration.toHours();
    	// 轉成天數，取餘
    		long days = hours / 24;          // 整數除法：取得完整的 24 小時數量
			

			// 防呆：不滿一天算一天
   			 if (days == 0) {
       		 days = 1;
    		}

		// 回傳：單價 x 天數
    	BigDecimal basePrice = plan.getBasePrice();
    	return basePrice.multiply(BigDecimal.valueOf(days));
		

	}

// ==========================================
// 私有方法2:長租計算單獨拉出來處理
// ==========================================
private BigDecimal calculateLongTermFee(AdminLongTermReqDto longTermReqDto, RentalPlansBean plan) {
    // 從長租明細 DTO 裡面拿出合約月數 //同時防無資料
    if (longTermReqDto == null || longTermReqDto.getContractMonths() == null) {
        throw new IllegalArgumentException("資料錯誤：長租訂單必須提供合約月數");
    }
    
    int months = longTermReqDto.getContractMonths();
    BigDecimal basePrice = plan.getBasePrice();
    
    // 回傳：單價 x 月數
    return basePrice.multiply(BigDecimal.valueOf(months));
}


// ==========================================
// 私有方法3:短租多的小時計費(供超過一天計算小時，並且評估用)
// ==========================================
private BigDecimal calculateShortTermOvertimeFee(AdminOrderReqDto rentalOrderDto, RentalPlansBean plan) {
    // 防呆：沒有時間就不算逾時費
    if (rentalOrderDto.getPickupTime() == null || rentalOrderDto.getReturnTime() == null) {
        return BigDecimal.ZERO; 
    }

    LocalDateTime pickupTime = rentalOrderDto.getPickupTime();
    LocalDateTime returnTime = rentalOrderDto.getReturnTime();
    
    Duration duration = Duration.between(pickupTime, returnTime);
    long hours = duration.toHours();
    long days = hours / 24;
    long extraHours = hours % 24; // 取餘數：剩下的小時數

    //如果是 0 天(已強制作為1天)，就不收逾時費
    if (days == 0) {
        extraHours = 0; 
    }

    // 檢查資料庫有沒有設定逾時費，沒有就當作 0
    BigDecimal overtimeFee = plan.getOvertimeFee();
    if (overtimeFee == null) {
        overtimeFee = BigDecimal.ZERO;
    }

	BigDecimal basePrice = plan.getBasePrice();
	if(basePrice ==null){
		basePrice = BigDecimal.ZERO;
	}

	BigDecimal overtimeFeeTotal = overtimeFee.multiply(BigDecimal.valueOf(extraHours));
	 //如果超過一天的錢就算一天，回傳basePrice
	 //如果不超過一天的錢算小時，回傳overtimeFeeTotal
	if(overtimeFeeTotal.compareTo(basePrice)>0){
		return basePrice;

	}else{
		return overtimeFeeTotal;

	}

    // 回傳的是：逾時單價 x 剩下的小時數
}


// ==========================================
// 私有方法4:驗證付款狀態與付款方式是否合理
// ==========================================
private void validatePaymentStatus(AdminOrderReqDto rentalOrderDto, BigDecimal deposit) {

	// 取得前端傳來的付款狀態，若前端沒傳，我們預設為「未付款」
	// 釋放控制權給前端（管理員）而非大於0時我們就強制改狀態
    PayStatus currentStatus = rentalOrderDto.getPayStatus();

	if (currentStatus == null) {
        currentStatus = PayStatus.UNPAID;
        rentalOrderDto.setPayStatus(currentStatus);
    }

	switch (currentStatus) {

		case UNPAID:
            // 狀態是未付，確保把付款方式清空，避免前端誤傳髒資料
            rentalOrderDto.setDepositPayMethod(null);
            rentalOrderDto.setBalancePayMethod(null);
            break;
		case  DEPOSIT_PAID:
			// 店員建單時直接標記「已付訂金」
			// 檢查 1：有訂金金額才需要付訂金
			if (deposit == null || deposit.compareTo(BigDecimal.ZERO) == 0) {
                 throw new IllegalArgumentException("資料錯誤：此單無需訂金，請勿標記為已付訂金");
            }
			// 檢查 2：必須填寫訂金是怎麼付的 (現金/信用卡/匯款等)
            if (rentalOrderDto.getDepositPayMethod() == null) {
                throw new IllegalArgumentException("資料錯誤：標記為已付訂金，但未提供訂金付款方式");
            }
			// 尾款還沒付，清空尾款付款方式
            rentalOrderDto.setBalancePayMethod(null);
            break;
		case  PAID:
			// 店員建單時直接標記「全額付清」
			if (rentalOrderDto.getDepositPayMethod() == null) {
                throw new IllegalArgumentException("資料錯誤：標記為結帳完成，但未提供訂金付款方式");
            }

			if (rentalOrderDto.getBalancePayMethod() == null) {
                throw new IllegalArgumentException("資料錯誤：標記為結帳完成，但未提供尾款付款方式");
            }
            break;

		default:
            throw new IllegalArgumentException("資料錯誤：未知的付款狀態");


	}	
		
}

// ==========================================
// 刪除的方法
// ==========================================

@Transactional
public void delete(int orderId) { // 連 OrderType 都不用傳進來了！
    
    // 直接刪除主表，JPA 會透過 CascadeType.REMOVE 自動幫你把對應的明細表也軟刪除！
    orderRepos.deleteById(orderId);
    
}

/*原先刪除寫法

@Transactional
    public void delete(int orderId, OrderType type) {
		
		// 1.判斷短租還是長租，然後刪除
		if (OrderType.SHORT_TERM.equals(type)) {
			shortTermRepos.deleteById(orderId);
        } else if (OrderType.LONG_TERM.equals(type)) {
        	longTermRepos.deleteById(orderId);
        }

        // 2. 刪除主表
		orderRepos.deleteById(orderId);
		
    }

*/
	
	
// ==========================================
// 更新的方法(捨棄不用)
// ==========================================
	@Transactional
	public RentalOrderBean update1(RentalOrderBean incomingOrder) {
		Optional<RentalOrderBean> originalorder = orderRepos.findById(incomingOrder.getOrderId());
		RentalOrderBean order;
		if(originalorder.isPresent()) {
			order= originalorder.get();
			/*雖然直接寫不會報錯，但如果這筆資料沒有，
			 * 會丟出NoSuchElementException: No value present
			 */
		}else {
			throw new EntityNotFoundException("找不到該訂單");
		}
		/*BeanUtils.copyProperties(來源物件, 目標物件, 排除的欄位...);
		來源物件覆蓋掉資料庫的物件，忽略特定欄位
		*/
		BeanUtils.copyProperties(incomingOrder, order, "orderTime", "orderId");
		
		return orderRepos.save(order);
	}

// ==========================================
// 更新的方法2
// ==========================================

public RentalOrderBean adminUpdateOrder(Integer orderId, AdminOrderUpdateReqDto orderUpdateReqDto) {

		//撈出原有的訂單
		RentalOrderBean existingOrder = orderRepos.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("找不到該訂單: " + orderId));

		BigDecimal originalRentalFee = existingOrder.getRentalFee();
    	BigDecimal originalExtraFee = existingOrder.getExtraFee();

		 boolean needsRecalculate = false;

		//撈出原有的方案，更新的時候觸發重算
		if (orderUpdateReqDto.getPlanId() != null) {
			RentalPlansBean newPlan = plansRepos.findById(orderUpdateReqDto.getPlanId())
			.orElseThrow(() -> new EntityNotFoundException("找不到該方案 ID: " + orderUpdateReqDto.getPlanId()));

			// 把整個物件塞進原本的 existingOrder
        	existingOrder.setRentalPlan(newPlan); 
        	needsRecalculate = true;
		}

		if (orderUpdateReqDto.getPickupTime() != null) {
        	existingOrder.setPickupTime(orderUpdateReqDto.getPickupTime());
       		needsRecalculate = true;
    	}

    	if (orderUpdateReqDto.getReturnTime() != null) {
        	existingOrder.setReturnTime(orderUpdateReqDto.getReturnTime());
        	needsRecalculate = true;
   		}

		//--------塞不太需要特殊檢查的欄位

		if (orderUpdateReqDto.getVehicleId() != null) {

			Vehicle newVehicle = vehicleRepo.findById(orderUpdateReqDto.getVehicleId())
			.orElseThrow(() -> new EntityNotFoundException("找不到該車輛: " + orderUpdateReqDto.getPlanId()));
			existingOrder.setVehicle(newVehicle);
		}

    	if (orderUpdateReqDto.getOrderStatus() != null) {
			existingOrder.setOrderStatus(orderUpdateReqDto.getOrderStatus());
		}
    	if (orderUpdateReqDto.getPayStatus() != null) {
			existingOrder.setPayStatus(orderUpdateReqDto.getPayStatus());
		}

		// 更新訂金付款方式 (Enum)
        if (orderUpdateReqDto.getDepositPayMethod() != null) {
            existingOrder.setDepositPayMethod(orderUpdateReqDto.getDepositPayMethod());
        }

		// 更新尾款付款方式 (Enum)
        if (orderUpdateReqDto.getBalancePayMethod() != null) {
            existingOrder.setBalancePayMethod(orderUpdateReqDto.getBalancePayMethod());
        }

		//更新訂單備註
		if (orderUpdateReqDto.getOrderRemark() != null) {
            existingOrder.setOrderRemark(orderUpdateReqDto.getOrderRemark());
        }

		if (orderUpdateReqDto.getInvoiceNo() != null) {
			existingOrder.setInvoiceNo((orderUpdateReqDto.getInvoiceNo()));
		}

		// 更新合約欄位
        if (orderUpdateReqDto.getContract() != null) {
            existingOrder.setContract(orderUpdateReqDto.getContract());
        }

		//--------(結束)塞不太需要檢查的欄位

		//處理上車地點
		if(orderUpdateReqDto.getPickupLocationId()!=null){

			Location pickupLocation = locationRepo.findById(orderUpdateReqDto.getPickupLocationId())
			.orElseThrow(()-> new EntityNotFoundException("找不到該取車地點ID" + orderUpdateReqDto.getPickupLocationId()));
			existingOrder.setPickupLocation(pickupLocation);
			needsRecalculate = true;
		}

		//處理還車地點
		if(orderUpdateReqDto.getReturnLocationId()!=null){
			Location returnLocation = locationRepo.findById(orderUpdateReqDto.getReturnLocationId())
			.orElseThrow(()->new EntityNotFoundException("找不到該還車地點ID"+orderUpdateReqDto.getReturnLocationId()));
			existingOrder.setReturnLocation(returnLocation);
			needsRecalculate = true;

		}

		//處理明細表
			//if確定這是一筆短租訂單，而且前端真的有傳明細資料過來
		if (existingOrder.getOrderType() == OrderType.SHORT_TERM && orderUpdateReqDto.getShortTerm() != null) {
        	ShortTermDetailBean stDetail = existingOrder.getShortTermDetail();//資料庫資料
			if (stDetail == null) {
    			throw new IllegalStateException("資料異常：找不到對應的短租明細，無法更新！");
			}
        	AdminShortTermUpdateDto stDto = orderUpdateReqDto.getShortTerm();//前端傳的新資料

			// -------塞資料和處理時間驗證，可以先塞再驗證，或是先驗證再進去，處理方式會略微不同
			//只是先塞資料再驗證比較直觀，如果不先塞資料驗證
			// 如果只傳其中一個數字(如還車)，另一個在呼叫get方法時噴錯(如取車)，
			// 需要拿未傳入的資料去比對

			if (stDto.getActualPickupTime() != null){
				//將前端傳來的actualPickupTime，塞進資料庫資料
				stDetail.setActualPickupTime(stDto.getActualPickupTime());
			}

			if (stDto.getActualReturnTime() != null) {
				stDetail.setActualReturnTime(stDto.getActualReturnTime());
				needsRecalculate = true;		
			}

			if (stDetail.getActualPickupTime() != null && stDetail.getActualReturnTime() != null) {
        		if (stDetail.getActualReturnTime().isBefore(stDetail.getActualPickupTime())) {
            throw new IllegalArgumentException("時間錯誤：實際還車時間不能早於實際取車時間！");
        		}
    		}
			// 塞資料和處理里程驗證

			if (stDto.getStartMileage() != null){
				stDetail.setStartMileage(stDto.getStartMileage());
				needsRecalculate = true;	
			} 
			if (stDto.getEndMileage() != null) {
				stDetail.setEndMileage(stDto.getEndMileage());
			}
			if (stDetail.getStartMileage() != null && stDetail.getEndMileage() != null) {
        		if (stDetail.getEndMileage() < stDetail.getStartMileage()) {
            	throw new IllegalArgumentException("里程錯誤：結束里程數 (" + stDetail.getEndMileage() + ") 不能小於起始里程數 (" + stDetail.getStartMileage() + ")！");
        		}
    		}

			if (stDto.getFuelLevelReturn() != null){

				stDetail.setFuelLevelReturn(stDto.getFuelLevelReturn());

			} 
			
        	if (stDto.getNoteDesc() != null) {
				stDetail.setNoteDesc(stDto.getNoteDesc());
			}

		}else if (existingOrder.getOrderType() == OrderType.LONG_TERM && orderUpdateReqDto.getLongTerm() != null) {
		
			LongTermDetailBean ltDetail = existingOrder.getLongTermDetail();//新的
			AdminLongTermUpdateDto ltDto = orderUpdateReqDto.getLongTerm();//舊的

			// 1. 驗證時間：實際還車不能早於實際取車
            LocalDateTime finalPickupTime = (ltDto.getActualPickupTime() != null) 
                                            ? ltDto.getActualPickupTime() : ltDetail.getActualPickupTime();
            LocalDateTime finalReturnTime = (ltDto.getActualReturnTime() != null) 
                                            ? ltDto.getActualReturnTime() : ltDetail.getActualReturnTime();

            if (finalPickupTime != null && finalReturnTime != null) {
                if (finalReturnTime.isBefore(finalPickupTime)) {
                    throw new IllegalArgumentException("時間錯誤：實際還車時間不能早於實際取車時間！");
                }
            }

            // 2. 驗證里程：結束里程不能小於起始里程
            Integer finalStartMileage = (ltDto.getStartMileage() != null) 
                                        ? ltDto.getStartMileage() : ltDetail.getStartMileage();
            Integer finalEndMileage = (ltDto.getEndMileage() != null) 
                                      ? ltDto.getEndMileage() : ltDetail.getEndMileage();

            if (finalStartMileage != null && finalEndMileage != null) {
                if (finalEndMileage < finalStartMileage) {
                    throw new IllegalArgumentException("里程錯誤：結束里程數 (" + finalEndMileage + ") 不能小於起始里程數 (" + finalStartMileage + ")！");
                }
            }

            // 3. 驗證合約與金額：防呆避免輸入負數
            if (ltDto.getContractMonths() != null && ltDto.getContractMonths() < 0) {
                throw new IllegalArgumentException("合約錯誤：合約月數不能為負數！");
            }
            if (ltDto.getMonthlyPayment() != null && ltDto.getMonthlyPayment().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("合約錯誤：月租金不能為負數！");
            }
            if (ltDto.getPaidMonths() != null && ltDto.getPaidMonths() < 0) {
                throw new IllegalArgumentException("合約錯誤：已付期數不能為負數！");
            }

			// ==========================================
            // 寫入區塊：確定資料乾淨，執行原本寫好的 Set 邏輯
            // ==========================================

            if (ltDto.getActualPickupTime() != null) {
                ltDetail.setActualPickupTime(ltDto.getActualPickupTime());
            }
                
            if (ltDto.getActualReturnTime() != null){
                ltDetail.setActualReturnTime(ltDto.getActualReturnTime());
            } 

            if (ltDto.getContractMonths() != null){
                ltDetail.setContractMonths(ltDto.getContractMonths());
				needsRecalculate = true;
				
            } 

            if (ltDto.getMonthlyPayment() != null) {
                ltDetail.setMonthlyPayment(ltDto.getMonthlyPayment());
            }

            if (ltDto.getPaidMonths() != null){
                ltDetail.setPaidMonths(ltDto.getPaidMonths());
            } 

            if (ltDto.getStartMileage() != null){
                ltDetail.setStartMileage(ltDto.getStartMileage());
            } 

            if (ltDto.getEndMileage() != null) {
                ltDetail.setEndMileage(ltDto.getEndMileage());
            }

            if (ltDto.getNoteDesc() != null) {
                ltDetail.setNoteDesc(ltDto.getNoteDesc());
            }
		}

		//金額處理邏輯 (系統計算 vs 手動覆蓋)
		// 【情況 1：透過方案和時間自動計算RentalFee和ExtraFee】

		if (needsRecalculate) {
        	RentalPlansBean currentPlan = existingOrder.getRentalPlan();

        	AdminOrderReqDto tempDtoForCalculate = new AdminOrderReqDto();
        	tempDtoForCalculate.setPickupTime(existingOrder.getPickupTime());
       	 	tempDtoForCalculate.setReturnTime(existingOrder.getReturnTime());

			// 處理租金 (時間的錢 = 基本天數 + 預定加時)

			BigDecimal standardRentalFee = BigDecimal.ZERO;

			if (existingOrder.getOrderType() == OrderType.SHORT_TERM) {
				// 把拿出來的 currentPlan 傳給計算方法
				BigDecimal base = calculateShortTermFee(tempDtoForCalculate, currentPlan);
                BigDecimal overtime = calculateShortTermOvertimeFee(tempDtoForCalculate, currentPlan);
                standardRentalFee = base.add(overtime);
				
			} else if (existingOrder.getOrderType() == OrderType.LONG_TERM) {
				AdminLongTermReqDto ltReq = new AdminLongTermReqDto();
                ltReq.setContractMonths(existingOrder.getLongTermDetail().getContractMonths());
                standardRentalFee = calculateLongTermFee(ltReq, currentPlan);
			}

			BigDecimal standardExtraFee = locationFeeService.getFee(
                existingOrder.getPickupLocation().getLocationId(),
                existingOrder.getReturnLocation().getLocationId()
            );

			// 寫入實體 (等待後方判斷是否手動覆蓋)
            existingOrder.setRentalFee(standardRentalFee);
            existingOrder.setExtraFee(standardExtraFee);


		}

		// 【情況 2：管理員手動覆蓋】(優先權最高，直接覆蓋 1 的結果或舊值)
		// 🌟 只有當前端傳來的值跟舊的不一樣，且沒有觸發自動重算時，才當作手動覆蓋

    		if (orderUpdateReqDto.getRentalFee() != null && orderUpdateReqDto.getRentalFee().compareTo(BigDecimal.ZERO) > 0) {
				// 這裡要跟 originalRentalFee (原始金額) 比對，如果一樣就代表沒改金額，就不執行
				if (!needsRecalculate || orderUpdateReqDto.getRentalFee().compareTo(originalRentalFee) != 0) {
					existingOrder.setRentalFee(orderUpdateReqDto.getRentalFee());
				}
			}

			if (orderUpdateReqDto.getExtraFee() != null && orderUpdateReqDto.getExtraFee().compareTo(BigDecimal.ZERO) > 0) {
				// 這裡要跟 originalExtraFee (原始金額) 比對
				if (!needsRecalculate || orderUpdateReqDto.getExtraFee().compareTo(originalExtraFee) != 0) {
					existingOrder.setExtraFee(orderUpdateReqDto.getExtraFee());
				}
		}

		//只要金額有被 A 或 B 動過，就必須重算總額與訂金
		// 為了安全，只要 orderUpdateReqDto 有傳金額相關或 needsRecalculate 為 true，就跑一遍
		//所以判斷總共有三個。判斷是否(1)方案、上下車時間是否有被動 (2)是否基本費被動過 (3)是否額外extraFee被動過

		if (needsRecalculate || orderUpdateReqDto.getRentalFee() != null || orderUpdateReqDto.getExtraFee() != null) {
        	BigDecimal newTotal = existingOrder.getRentalFee().add(existingOrder.getExtraFee());
        	existingOrder.setTotalAmount(newTotal);

       	 	BigDecimal newDeposit = newTotal.multiply(new BigDecimal("0.1"))
                                        .setScale(0, RoundingMode.HALF_UP);
        	existingOrder.setDeposit(newDeposit);
            
            // 補上尾款更新 (與你 insert 方法裡的邏輯保持一致)
            BigDecimal newRemaining = newTotal.subtract(newDeposit);
            existingOrder.setRemainingBalance(newRemaining);
    	}

	 return orderRepos.save(existingOrder);

}



// ==========================================
// 篩選的方法
// ==========================================
		// 輔助判斷字串是否有值
		private boolean hasValue(String s) {
			return s != null && !s.trim().isEmpty();
		}

		// 接收前端傳來的 Map，動態組裝查詢條件
		@Transactional(readOnly = true)
		public List<RentalOrderBean> filterOrders(Map<String, String> map) {
			
			/*
			Specification：這是 Spring JPA 提供的一個介面，
			中文可以翻作「查詢規格書」。告訴系統這是一個用來產生 SQL 條件的物件。*/
			
			Specification<RentalOrderBean> spec = (root, query, cb) -> {
				/*
				 * root：代表「根源」。用來抓取資料庫裡的實體欄位（相當於 SQL 的 FROM）。
				 * query：代表「查詢整體」。用來做更進階的設定，例如分組或排序（這裡沒用到）。
				 * cb：全名是 CriteriaBuilder（條件建造者）。這是你的「工具箱」，負責產生 =, >, < 等判斷條件。
				 * 完全可以自己改名！它只是變數名稱。只是最好按照約定俗成的方式寫這三個
				 */
				
				
				List<Predicate> predicates = new ArrayList<Predicate>();
				/*
				 * 告訴這個清單裡面只能裝 Predicate。Predicate 在這裡的意思是「單一的 WHERE 條件」。
				 */

				// (1) 客戶 ID (因為目前沒有 JOIN CustomerBean，改查精確的 custId，
				//之後串接客戶表可以改name)
				if (hasValue(map.get("custId"))) {
					predicates.add(cb.equal(root.get("custId"), Integer.valueOf(map.get("custId"))));
					/*
					 * Integer.valueOf(map.get("custId"))為拿到前端字串轉數字
					 * root.get("custId")：這代表資料庫 RentalOrderBean 裡面的那個 custId 欄位。
					 * cb.equal( 核心2 , 核心1 )
					這個括號把前後兩個東西包起來，翻譯成 SQL 就是：WHERE custId = 123。現在這整包變成了一個 Predicate（條件）。
					cb.equal() 會產生一個 Predicate（條件）。
					cb.and() 或 cb.or() 產生出來的東西，也是一個 Predicate！	
					*/
				}

				// (2) 車輛編號
				if (hasValue(map.get("vehicleId"))) {
					predicates.add(cb.equal(root.get("vehicleId"), Integer.valueOf(map.get("vehicleId"))));
				}

				// (3) 訂單類型
				if (hasValue(map.get("orderType"))) {
					predicates.add(cb.equal(root.get("orderType"), map.get("orderType")));
				}

				// (4) 方案編號
				if (hasValue(map.get("planId"))) {
					predicates.add(cb.equal(root.get("planId"), Integer.valueOf(map.get("planId"))));
				}

				// (5) 取車據點
				if (hasValue(map.get("pickupLocation"))) {
					predicates.add(cb.equal(root.get("pickupLocationId"), Integer.valueOf(map.get("pickupLocation"))));
				}

				// (6) 還車據點
				if (hasValue(map.get("returnLocation"))) {
					predicates.add(cb.equal(root.get("returnLocationId"), Integer.valueOf(map.get("returnLocation"))));
				}

				// (7) 基礎租金 > 0
				if ("true".equals(map.get("rentalFee"))) {
					predicates.add(cb.greaterThan(root.get("rentalFee"), BigDecimal.ZERO));
				}

				// (8) 額外費用 > 0
				if ("true".equals(map.get("extraFee"))) {
					predicates.add(cb.greaterThan(root.get("extraFee"), BigDecimal.ZERO));
				}

				// (9) 訂金 = 0
				if ("true".equals(map.get("deposit"))) {
					predicates.add(cb.equal(root.get("deposit"), BigDecimal.ZERO));
				}

				// (10) 付款狀態
				if (hasValue(map.get("payStatus"))) {
					predicates.add(cb.equal(root.get("payStatus"), map.get("payStatus")));
				}

				// (11) 訂單狀態
				if (hasValue(map.get("orderStatus"))) {
					predicates.add(cb.equal(root.get("orderStatus"), map.get("orderStatus")));
				}

				// (12) 付款方式
				if (hasValue(map.get("payMethod"))) {
					predicates.add(cb.equal(root.get("paymentMethod"), map.get("payMethod")));
				}

				//(13) 新增：訂單備註 (模糊搜尋 Keyword)
                if (hasValue(map.get("orderRemark"))) {
                    // cb.like 會轉成 SQL 的 LIKE '%關鍵字%'
                    predicates.add(cb.like(root.get("orderRemark"), "%" + map.get("orderRemark") + "%"));
                }

				// 將所有條件用 AND 連接
				return cb.and(predicates.toArray(new Predicate[0]));
				/*
				 * cb.and：工具箱裡的另一個工具，
				 * 負責把括號裡面的「所有條件」，用 SQL 的 AND 串接起來。
				 * 
				 * predicates：我們的那個籃子（裡面可能裝了 3 個條件，也可能都沒裝）。
				 * .toArray：因為 cb.and 很龜毛，它不收「清單（List）」，只收「陣列（Array）」，
				 * 所以要把籃子裡的東西倒出來變成陣列。
				 * 
				 * new Predicate[0]：這是一個標準寫法。意思是「請產生一個長度為 0 的空陣列，作為型態範本」。
				 * Java 發現籃子裡如果有 3 個東西，它會自動把這個 0 擴充成 3，然後幫你裝好。
				 * 
				 * 
				 * 那為什麼可以擴充？
				 * Java 直接把這個長度為 0 的陣列丟進垃圾桶。
				 * Java 利用反射機制（Reflection），偷偷看了一眼你丟掉的那個陣列是什麼型態（喔，是 Predicate 型態）。
				 * 
				 * 
				 * 
				 * 回傳的不是給 spec，你是回傳給 Specification 裡面的那個『隱形方法』
				 * 
				 */
				
				
			};

			return orderRepos.findAll(spec);
		}

		
// ==========================================
// 車輛、訂單生命週期處理
// ==========================================
		/*
		p1 取車時狀態管理
		pickupOrder(int orderId) 取車：需要將訂單狀態更新為「進行中（RENTING）」，記錄實際取車時間，並可能連動更新車輛狀態為「出租中」。
		*/
		@Transactional
		public RentalOrderBean pickupOrder(int orderId, Integer startMileage, String note){
			// 1. 撈出原有的訂單
			RentalOrderBean orderBean = orderRepos.findById(orderId)
			.orElseThrow(()->new EntityNotFoundException("找不到該訂單ID:"+orderId));

			// 2. 狀態防呆驗證：確保訂單狀態是「已預約 (RESERVED)」

			if(orderBean.getOrderStatus()!=null && orderBean.getOrderStatus()!=OrderStatus.RESERVED){
				throw new IllegalStateException("操作失敗：該訂單狀態為 " + orderBean.getOrderStatus() + "，無法執行取車作業！");

			}

			// 3. 記錄實際取車時間 (以當下系統時間為主)
			LocalDateTime now = LocalDateTime.now();
			String formattedNote = (note != null && !note.trim().isEmpty()) ? "[取車] " + note.trim() : null;
			if(orderBean.getOrderType()==OrderType.SHORT_TERM){
				ShortTermDetailBean stDetail = orderBean.getShortTermDetail();

				if(stDetail!=null){
					if(stDetail.getActualPickupTime()==null){
						stDetail.setActualPickupTime(now);
					}
					if(startMileage != null) stDetail.setStartMileage(startMileage); // 寫入里程
					if(formattedNote != null) stDetail.setNoteDesc(formattedNote); // 寫入備註
				}
			}else if(orderBean.getOrderType()==OrderType.LONG_TERM){
				LongTermDetailBean ltDetail = orderBean.getLongTermDetail();

				if(ltDetail!=null){
					if(ltDetail.getActualPickupTime()==null){
						ltDetail.setActualPickupTime(now);
						if(startMileage != null) ltDetail.setStartMileage(startMileage); // 寫入里程
						if(formattedNote != null) ltDetail.setNoteDesc(formattedNote); // 寫入備註
					}
				}
			}
			// 4. 更新訂單狀態為「已取車 (PICKED_UP)」
			orderBean.setOrderStatus(OrderStatus.PICKED_UP);


			// 5. 連動更新車輛狀態為「出租中 (RENTED)」
			Vehicle vehicle = orderBean.getVehicle();
			if(vehicle != null){

				vehicleService.updateVehicleStatus(orderBean.getVehicle().getVehicleId(),VehicleStatus.RENTING);
				
			}

			// 6. 存檔並回傳更新後的訂單
    		return orderRepos.save(orderBean);
		}


		/*
		p2 還車狀態管理
		returnOrder(int orderId) 還車：記錄實際還車時間，並觸發「逾時費計算」邏輯（如果有超時），更新車輛狀態為「待清潔/可租用」。
		*/
		
		@Transactional
		public RentalOrderBean returnOrder(int orderId, Integer endMileage, FuelLevel fuel, String note){
			// 1. 撈出原有的訂單
			RentalOrderBean orderBean = orderRepos.findById(orderId)
			.orElseThrow(()->new EntityNotFoundException("找不到該訂單ID:"+orderId));
			// 2.防呆狀態
			
			if(orderBean.getOrderStatus() != null && 
			orderBean.getOrderStatus() != OrderStatus.PICKED_UP && 
			orderBean.getOrderStatus() != OrderStatus.OVERDUE){
				throw new IllegalStateException("操作失敗：該訂單狀態為 " + orderBean.getOrderStatus() + "，無法執行還車作業！");	
			}
			//3.紀錄還車時間
			LocalDateTime now = LocalDateTime.now();
			String formattedNote = (note != null && !note.trim().isEmpty()) ? "[還車] " + note.trim() : "";
			if(orderBean.getOrderType()==OrderType.SHORT_TERM){
				ShortTermDetailBean stDetail = orderBean.getShortTermDetail();
				if(stDetail!=null){
					if(stDetail.getActualReturnTime()==null){
						stDetail.setActualReturnTime(now);
					}
					// 里程防呆驗證
					if(endMileage != null){
						if(stDetail.getStartMileage() != null && endMileage < stDetail.getStartMileage()){
							throw new IllegalArgumentException("里程錯誤：還車里程 (" + endMileage + ") 不能小於出車里程 (" + stDetail.getStartMileage() + ")！");
						}
						stDetail.setEndMileage(endMileage);
					}
					
					// 寫入油量
					if(fuel != null) {
						stDetail.setFuelLevelReturn(fuel);
					}
					// 拼接備註：把舊的備註拿出來，接上新的備註
					if (!formattedNote.isEmpty()) {
						String oldNote = (stDetail.getNoteDesc() == null) ? "" : stDetail.getNoteDesc() + " ";
						stDetail.setNoteDesc(oldNote + formattedNote);
					}
				
				}
			}else if(orderBean.getOrderType()==OrderType.LONG_TERM){
				LongTermDetailBean ltDetail = orderBean.getLongTermDetail();
				if(ltDetail!=null){
					if(ltDetail.getActualPickupTime()==null){
						ltDetail.setActualPickupTime(now);
					}
					// 里程防呆驗證
					if(endMileage != null){
						if(ltDetail.getStartMileage() != null && endMileage < ltDetail.getStartMileage()){
							throw new IllegalArgumentException("里程錯誤：還車里程不能小於出車里程！");
						}
						ltDetail.setEndMileage(endMileage);
					}
					// 拼接備註
					if (!formattedNote.isEmpty()) {
						String oldNote = (ltDetail.getNoteDesc() == null) ? "" : ltDetail.getNoteDesc() + " ";
						ltDetail.setNoteDesc(oldNote + formattedNote);
					}
					
				}
			}
			// ==========================================
			// 新增：費用結算區塊 (油錢、超里程、實際超時)
			// ==========================================
			BigDecimal addedFuelFee = BigDecimal.ZERO;
			BigDecimal addedMileageFee = BigDecimal.ZERO;
			BigDecimal addedOvertimeFee = BigDecimal.ZERO;
			RentalPlansBean plan = orderBean.getRentalPlan();
			LocalDateTime expectedReturn = orderBean.getReturnTime();
			LocalDateTime expectedPickup = orderBean.getPickupTime();
			if (plan != null && expectedPickup != null && expectedReturn != null) {
				// --- 1. 計算油錢 (以公定價為例，可自行調整) ---
				if (fuel != null) {
					switch (fuel) {
						case EMPTY: addedFuelFee = new BigDecimal("500"); break;
						case LOW:   addedFuelFee = new BigDecimal("300"); break;
						case HALF:  addedFuelFee = new BigDecimal("150"); break;
						default:    addedFuelFee = BigDecimal.ZERO; break;
					}
				}
				// --- 2. 計算實際超時費 ---
				if (now.isAfter(expectedReturn)) {
					long lateHours = java.time.Duration.between(expectedReturn, now).toHours();
					if (lateHours > 0 && plan.getOvertimeFee() != null) {
						addedOvertimeFee = plan.getOvertimeFee().multiply(BigDecimal.valueOf(lateHours));
					}
				}
				// --- 3. 計算超里程費 ---
				if (endMileage != null && plan.getMileageLimit() != null && plan.getExcessMileageFee() != null) {
					
					// 加入防呆：先確定明細表真的存在，再去抓起始里程
					Integer startM = null;
					if (orderBean.getOrderType() == OrderType.SHORT_TERM) {
						if (orderBean.getShortTermDetail() != null) {
							startM = orderBean.getShortTermDetail().getStartMileage();
						}
					} else {
						if (orderBean.getLongTermDetail() != null) {
							startM = orderBean.getLongTermDetail().getStartMileage();
						}
					}
					if (startM != null && endMileage > startM) {
						long rentDays = java.time.Duration.between(expectedPickup, expectedReturn).toDays();
						if (rentDays == 0) rentDays = 1;
						int totalFreeMileage = plan.getMileageLimit() * (int) rentDays;
						int actualDriven = endMileage - startM;
						if (actualDriven > totalFreeMileage) {
							int overMileage = actualDriven - totalFreeMileage;
							addedMileageFee = plan.getExcessMileageFee().multiply(BigDecimal.valueOf(overMileage));
						}
					}
				}
			}
			// --- 4. 總結算並寫入資料庫與備註 ---
			BigDecimal totalNewExtraFee = addedFuelFee.add(addedMileageFee).add(addedOvertimeFee);
			if (totalNewExtraFee.compareTo(BigDecimal.ZERO) > 0) {
				// 將新算出的費用加進額外費用
				BigDecimal oldExtraFee = (orderBean.getExtraFee() != null) ? orderBean.getExtraFee() : BigDecimal.ZERO;
				orderBean.setExtraFee(oldExtraFee.add(totalNewExtraFee));
				
				// 更新總金額與尾款
				BigDecimal newTotal = orderBean.getRentalFee().add(orderBean.getExtraFee());
				orderBean.setTotalAmount(newTotal);
				
				BigDecimal deposit = (orderBean.getDeposit() != null) ? orderBean.getDeposit() : BigDecimal.ZERO;
				orderBean.setRemainingBalance(newTotal.subtract(deposit));
				// 產生明細字串並塞入備註
				StringBuilder feeDetail = new StringBuilder();
				feeDetail.append(" [還車結算加收：");
				if (addedFuelFee.compareTo(BigDecimal.ZERO) > 0) feeDetail.append("油資補貼 $").append(addedFuelFee).append(" ");
				if (addedMileageFee.compareTo(BigDecimal.ZERO) > 0) feeDetail.append("超里程費 $").append(addedMileageFee).append(" ");
				if (addedOvertimeFee.compareTo(BigDecimal.ZERO) > 0) feeDetail.append("逾時費 $").append(addedOvertimeFee).append(" ");
				feeDetail.append("]");
				if (orderBean.getOrderType() == OrderType.SHORT_TERM) {
					String currentNote = orderBean.getShortTermDetail().getNoteDesc();
					orderBean.getShortTermDetail().setNoteDesc((currentNote == null ? "" : currentNote) + feeDetail.toString());
				} else {
					String currentNote = orderBean.getLongTermDetail().getNoteDesc();
					orderBean.getLongTermDetail().setNoteDesc((currentNote == null ? "" : currentNote) + feeDetail.toString());
				}
			}
			// ==========================================
			// 結算區塊結束
			// ==========================================
			//4.更新訂單狀態為「已歸還(待檢查) (RETURNED)」
			orderBean.setOrderStatus(OrderStatus.RETURNED);
			//5.更新車輛狀態和車輛里程數
			Vehicle vehicle = orderBean.getVehicle();
			if(vehicle != null){
				vehicleService.updateVehicleStatus(orderBean.getVehicle().getVehicleId(),VehicleStatus.CLEANING);
				// vehicleService.updateVehicleMileage(orderBean.getVehicle().getVehicleId(), endMileage);
				
			}
			// 6. 存檔並回傳更新後的訂單
			return orderRepos.save(orderBean);
		}

		/*p3 結算狀態管理，傳給F3資訊
		settleOrder(int orderId) 結算：處理尾款（remainingBalance）的支付邏輯，並將付款狀態（payStatus）更新為 PAID，訂單狀態改為 COMPLETED。
		*/
		public RentalOrderBean closeOrder(int orderId, PaymentMethod balancePayMethod){

			// 1. 撈出原有的訂單
			RentalOrderBean orderBean = orderRepos.findById(orderId)
			.orElseThrow(()->new EntityNotFoundException("找不到該訂單ID:"+orderId));

			// 2.防呆狀態
			if(orderBean.getOrderStatus()!=null && orderBean.getOrderStatus()!=OrderStatus.RETURNED){
				throw new IllegalStateException("操作失敗：該訂單狀態為 " + orderBean.getOrderStatus() + "，無法執行結算作業！");	
			}

			if(balancePayMethod == null){
				throw new IllegalArgumentException("請選擇尾款付款方式");
			}

			// 3. 更新付款狀態為「支付完成 (PAID)」
    		orderBean.setPayStatus(PayStatus.PAID);

			// 4. 更新訂單狀態為「已結案 (CLOSED)」
    		orderBean.setOrderStatus(OrderStatus.CLOSED);

			// 4-1結案時寫入尾款付款方式
			orderBean.setBalancePayMethod(balancePayMethod);

			// 5.利用F3將訂單id和客戶id傳入
			// (存第一次，或是存點數)

			long historyCount = orderRepos.countByCustomer_CustIdAndOrderStatus(orderBean.getCustomer().getCustId(), OrderStatus.CLOSED);
			/*
			countBy翻譯成 SQL：SELECT COUNT(*) FROM rental_order WHERE...
			Customer_CustId翻譯成 SQL：... cust_id = ?
			(底線 _ 的意思是：去找 RentalOrder 實體裡面的 customer 物件，再找裡面的 custId)
			And -> ... AND ...
			OrderStatus->... order_status = ?
			*/

			if (historyCount == 0){
				pointsEventService.addFixedPoints(orderBean.getCustomer().getCustId(),ChangeType.FIRST_RENTAL);
				pointsEventService.addPoints(orderBean.getCustomer().getCustId(),orderBean.getOrderId(),ChangeType.RENTAL);

			}else{
				pointsEventService.addPoints(orderBean.getCustomer().getCustId(),orderBean.getOrderId(),ChangeType.RENTAL);
			}



			// 6. 存檔並回傳
    		return orderRepos.save(orderBean);
		}

		/*p4取消訂單管理，釋放鎖定車輛

		cancelOrder(int orderId) 取消：將訂單標記為取消，釋放當初鎖定的車輛（還原為 AVAILABLE），並處理訂金的退款或沒收邏輯。
		*/

		public RentalOrderBean cancelOrder(int orderId){

			// 1. 撈出原有的訂單
			RentalOrderBean orderBean = orderRepos.findById(orderId)
			.orElseThrow(()->new EntityNotFoundException("找不到該訂單ID:"+orderId));

			// 2. 狀態防呆驗證：確保訂單狀態是「已預約 (RESERVED)」才能取消
			if (orderBean.getOrderStatus() != null && orderBean.getOrderStatus()!=OrderStatus.RESERVED) {
				throw new IllegalStateException("操作失敗：該訂單狀態為 " + orderBean.getOrderStatus() + "，無法執行取消作業！");
			}
			// 3. 處理訂金的退款邏輯 (改變付款狀態)

			if (orderBean.getPayStatus() != null && orderBean.getPayStatus()== PayStatus.DEPOSIT_PAID) {
				// 情況 A：如果客人已經付了訂金，取消訂單後，錢要退給人家
				orderBean.setPayStatus(PayStatus.REFUNDING); // 狀態改為「退款中」
			} else {
				// 情況 B：如果客人根本還沒付錢 (UNPAID)，就無所謂退款，維持未付款即可
				// 這裡不做任何動作
			}

			// 4. 將訂單標記為取消
    		orderBean.setOrderStatus(OrderStatus.CANCELLED);

			return orderRepos.save(orderBean);



		}
		
		
		/*p5完成退款作業
		*/

		@Transactional
		public RentalOrderBean completeRefund(int orderId) {
			
			// 1. 撈出原有的訂單
			RentalOrderBean orderBean = orderRepos.findById(orderId)
				.orElseThrow(() -> new EntityNotFoundException("找不到該訂單 ID: " + orderId));

			// 2. 防呆驗證：確保目前真的是「退款中」才能改成「已退款」
			if (orderBean.getPayStatus() != null && orderBean.getPayStatus()!=PayStatus.REFUNDING) {
				throw new IllegalStateException("操作失敗：目前狀態非退款中，無法執行此動作！");
			}

			// 3. 更新付款狀態為「已退款 (REFUNDED)」
			orderBean.setPayStatus(PayStatus.REFUNDED); 

			// 4. 存檔並回傳
			return orderRepos.save(orderBean);
		}
// ======================================================================
// 私有方法：根據時間取出可用車輛 (附加業務邏輯過濾)，這邊要寫給客戶端我的新增方法
// ========================================================================
		
		/**
		 * 自動配對方案：根據車輛的 vehicleType + 租期天數，自動找到對應的 rental_plan
		 * 取代原本的 validateCarEligibility（因為現在每台車都能日租也能長租）
		 */
		private RentalPlansBean autoMatchPlan(Vehicle vehicle, LocalDateTime pickupTime, LocalDateTime returnTime) {
			// 1. 計算天數
			long diffDays = Duration.between(pickupTime, returnTime).toDays();
			boolean isLongTerm = diffDays >= 30;
			// 2. 拿車的車型
			if (vehicle.getCarModel() == null) {
				throw new RuntimeException("此車輛未綁定車款，無法出租！");
			}
			String vehicleType = vehicle.getCarModel().getVehicleType();
			// 3. 用車型 + 長短租去 rental_plans 找方案
			return plansRepos.findByAppliedVehicleTypeAndIsLongTerm(vehicleType, isLongTerm)
				.orElseThrow(() -> new RuntimeException("找不到符合的方案，車型：" + vehicleType));
		}





		/*這裡不寫，直接在extraFee使用F6裡的CrossLocationFeeService Api
		調度費用	
		calculateCrossLocationFee() 跨區/甲租乙還費用：目前你已經有短租、長租與逾時費（calculateShortTermOvertimeFee）
		*/

		/*
		根據會員 ID 查詢歷史訂單」的方法
		（例如：findByCustId(int custId)）
		
		*/
	
	
	

}
