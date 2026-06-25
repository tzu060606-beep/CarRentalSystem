package com.carrental.system.usedCar.EcPay;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carrental.system.rentalorder.repository.RentalOrderRepository;
import com.carrental.system.transfer.repository.TransferOrderRepository;
import com.carrental.system.usedCar.DTO.SalesRecordDto;
import com.carrental.system.usedCar.repository.SalesRecordRepository;
import com.carrental.system.usedCar.service.SalesRecordService;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

	@Autowired
	private EcpayConfig ecpayConfig;

	@Autowired
	private SalesRecordRepository salesRepo;

	@Autowired
	private RentalOrderRepository rentalRepo;

	@Autowired
	private TransferOrderRepository transferRepo;

	@Autowired
	private SalesRecordService salesRecordService;

	/**
	 * 前端呼叫此 API 啟動結帳流程
	 */
	@PostMapping("/checkout")
	public Map<String, String> checkout(@RequestBody OrderRequest request) {

		int finalAmount = 0;
		String itemName = "";

		// --- 1. 根據業務類型撈取資料 ---
		switch (request.getCategory()) {
		case "SALES":
			Integer saleId = request.getId().intValue();
			SalesRecordDto saleDto = salesRecordService.selectById(saleId);
			if (saleDto == null)
				throw new RuntimeException("找不到成交紀錄 ID: " + saleId);

			finalAmount = saleDto.getFinalPrice().intValue();
			itemName = "UsedCarSaleNo" + saleDto.getSaleId(); // 盡量使用英數
			break;

		// 組員對接預留區
		case "RENTAL": // 租車
//              var rental = rentalRepo.findById(request.getId())
//                          .orElseThrow(() -> new RuntimeException("找不到租車訂單"));
//              finalAmount = rental.getTotalPrice(); // 連動租車總金額
//              itemName = "租車費用付款 - 單號:" + rental.getRentalOrderId();
//              break;
//
//      case "TRANSFER": // 專車接送
//           var transfer = transferRepo.findById(request.getId())
//                          .orElseThrow(() -> new RuntimeException("找不到接送訂單"));
//           finalAmount = transfer.getPrice(); // 連動接送費用
//           itemName = "專車接送服務 - 單號:" + transfer.getTransferId();
//           break;
			// case "RENTAL": ... break;
			// case "TRANSFER": ... break;

		default:
			throw new RuntimeException("未知的業務類別");
		}

		// --- 2. 封裝綠界所需參數 ---
		// --- 封裝綠界所需參數 ---
		Map<String, String> params = new HashMap<>();
		params.put("MerchantID", ecpayConfig.getPaymentMerchantId());
		params.put("PaymentType", "aio");
		params.put("EncryptType", "1");
		params.put("ChoosePayment", "ALL");

		// 交易編號：純英數，不超過 20 碼
		params.put("MerchantTradeNo", "C" + System.currentTimeMillis()); 
		params.put("MerchantTradeDate", new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));

		// 金額：確保 finalAmount > 0 且為整數
		if(finalAmount > 200000) {
			finalAmount /= 10;
		}
		params.put("TotalAmount", String.valueOf(finalAmount));

		// 商品名稱與描述：不可為空，建議暫時用簡單英數
		// 如果前端有傳自定義名稱就用前端的，否則用預設的
		String finalItemName = request.getCustomItemName();
		if (finalItemName == null || finalItemName.isEmpty()) {
		    finalItemName = "UsedCarOrder" + request.getId();
		}

		// 移除特殊符號 (這步很重要，避免前端亂傳導致簽章失敗)
		finalItemName = finalItemName.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9 ]", "");

		params.put("ItemName", finalItemName);
		params.put("TradeDesc", "AutomobilePayment"); 

		// 自定義欄位（給 Callback 用）
		params.put("CustomField1", request.getCategory());
		params.put("CustomField2", String.valueOf(request.getId()));

		// URL 設定
		params.put("ReturnURL", " https://asparagus-tricycle-jalapeno.ngrok-free.dev/api/payment/callback");
		params.put("ClientBackURL", "http://localhost:5173/usedcarshop");

		// 最後生成簽章
		String checkMacValue = EcpayUtil.generateCheckMacValue(params, 
		        ecpayConfig.getPaymentHashKey(),
		        ecpayConfig.getPaymentHashIv());
		params.put("CheckMacValue", checkMacValue);


		return params;
	}

	/**
	 * 綠界付款成功後的自動回傳 (Callback)
	 */
	
	@PostMapping("/callback")
	public String paymentCallback(@RequestParam Map<String, String> result) {

		// RtnCode "1" 代表付款成功
		if ("1".equals(result.get("RtnCode"))) {

			// 直接從自定義欄位拿資料，不再需要拆解 MerchantTradeNo
			String category = result.get("CustomField1");
			Integer id = Integer.parseInt(result.get("CustomField2"));

			System.out.println("收到付款成功回傳: 類別=" + category + ", ID=" + id);

			// --- 執行資料庫更新 ---
			if ("SALES".equals(category)) {
				salesRepo.updateStatusToPaid(id);
			}
//		} else if ("RENTAL".equals(category)) {
//          rentalRepo.updateStatusToPaid(id); // 更新 rental_order 狀態
//      } else if ("TRANSFER".equals(category)) {
//          transferRepo.updateStatusToPaid(id); // 更新 transfer_order 狀態

			return "1|OK"; // 必須回傳這個，綠界才不會重複發送通知
		}

		return "0|Error";
	}
}

