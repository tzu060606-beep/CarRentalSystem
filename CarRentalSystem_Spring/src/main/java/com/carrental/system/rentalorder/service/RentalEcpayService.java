package com.carrental.system.rentalorder.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carrental.system.rentalorder.entity.RentalOrderBean;
import com.carrental.system.rentalorder.enums.PayStatus;
import com.carrental.system.rentalorder.enums.PaymentMethod;
import com.carrental.system.rentalorder.repository.RentalOrderRepository;
import com.carrental.system.usedCar.EcPay.EcpayConfig;
import com.carrental.system.usedCar.EcPay.EcpayUtil;
import org.springframework.context.ApplicationEventPublisher;
import com.carrental.system.rentalorder.mail.RentalOrderSuccessMailEvent;


@Service
public class RentalEcpayService {

    private static final String CUSTOM_FIELD_CATEGORY = "RENTAL_DEPOSIT";
    private static final String DEFAULT_RETURN_URL = "https://pennant-stench-afternoon.ngrok-free.dev/api/rental/payment/ecpay/callback";
    private static final String DEFAULT_CLIENT_BACK_URL = "http://localhost:5173/orders/custsuccess";

    @Autowired
    private EcpayConfig ecpayConfig;

    @Autowired
    private RentalOrderRepository rentalOrderRepository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Transactional(readOnly = true)
    public Map<String, Object> createRentalDepositCheckout(Integer orderId, PaymentMethod paymentMethod, String returnUrl, String clientBackUrl) {
        // 前端點擊結帳->系統包裝綠界表單
        // 訂單驗證與安全攔截
        // 1. 從資料庫把訂單撈出來，預防前端傳入不存在的幽靈 ID
        RentalOrderBean order = rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("找不到租車訂單 ID: " + orderId));

        // 2. 金額防呆攔截，檢查是不是 0 元或負數，防止系統被白嫖
        BigDecimal deposit = order.getDeposit();
        if (deposit == null || deposit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("此訂單沒有可支付的押金");
        }


        // 3. 支付工具規格化，預設防呆為信用卡
        PaymentMethod normalizedPaymentMethod = paymentMethod == null ? PaymentMethod.CREDIT_CARD : paymentMethod;
        String choosePayment = toEcpayChoosePayment(normalizedPaymentMethod);

        /*

        原理解說：當客人在前端點擊「支付押金」時，
        非同步請求（Axios/Fetch）會把訂單 ID 送到後端。這一步是安全大門，
        必須確認資料庫真的有這筆單，且押金金額大於 0。
        這對下一步的影響是：確保我們拿到的金額是絕對真實的，才可以開始跟綠界報價。
        
        */
        // ----------------------組裝綠界特規黑盒子（formData）
        // 組裝綠界特規黑盒子（formData）
        // 4. 建立一個專門準備塞給綠界的參數（全部都要轉成 String）

        Map<String, String> formData = new HashMap<>();
        formData.put("MerchantID", ecpayConfig.getPaymentMerchantId());// 帶入特店編號
        formData.put("PaymentType", "aio");// 綠界規定固定傳 aio
        formData.put("EncryptType", "1");// 綠界規定固定傳 1 (代表 SHA256 加密)
        formData.put("ChoosePayment", choosePayment);

        // 5. 動態組合高強度不重複的綠界交易單號 (加上時間戳記)
        formData.put("MerchantTradeNo", buildMerchantTradeNo(orderId));
        formData.put("MerchantTradeDate", new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));

        // 6. 將金錢轉換為無小數點的整數（綠界不收小數點）
        formData.put("TotalAmount", String.valueOf(deposit.setScale(0, RoundingMode.HALF_UP).intValue()));
        formData.put("TradeDesc", sanitizeEcpayText("OneRentRentalDeposit"));
        formData.put("ItemName", sanitizeEcpayText("OneRent租車押金訂單" + orderId));

        // 7. 設定綠界的異步回傳網址（ReturnURL）與客人的跳轉網址（ClientBackURL）
        formData.put("ReturnURL", hasText(returnUrl) ? returnUrl : DEFAULT_RETURN_URL);
        formData.put("ClientBackURL", hasText(clientBackUrl) ? clientBackUrl : DEFAULT_CLIENT_BACK_URL);
        
        // 8. 靈魂小技巧：利用三個口袋（CustomField）把內部機密（訂單 ID、付款管道）藏進去送給綠界
        formData.put("CustomField1", CUSTOM_FIELD_CATEGORY);
        formData.put("CustomField2", String.valueOf(orderId));
        formData.put("CustomField3", normalizedPaymentMethod.name());
        // CustomField 這三個口袋非常聰明，因為綠界扣完錢後，
        // 會把這三個口袋原封不動地還給我們，這樣我們回傳時才知道是誰付了錢！


        // 簽署安全檢查碼（CheckMacValue）
        // 9. 呼叫工具類別，把剛剛排序、Encode、SHA256 攪拌完的檢查碼算出來
        String checkMacValue = EcpayUtil.generateCheckMacValue(
                formData,
                ecpayConfig.getPaymentHashKey(),
                ecpayConfig.getPaymentHashIv()
        );
        formData.put("CheckMacValue", checkMacValue);

        // 10. 打包綠界收錢網址與 formData 回傳給前端 Vue
        Map<String, Object> response = new HashMap<>();
        response.put("paymentUrl", ecpayConfig.getPaymentUrl());
        response.put("formData", formData);
        return response;
    }

    // 嚴格的保全驗證:客戶在綠界刷卡成功 ->綠界背後通知我

    @Transactional
    public boolean handleRentalDepositCallback(Map<String, String> callbackParams) {
        // 11. 重新算一次檢查碼，跟綠界傳過來的比對。只要少一個字、金額不對，立刻拒絕！
        if (!isValidCheckMacValue(callbackParams)) {
            throw new IllegalArgumentException("綠界 CheckMacValue 驗證失敗");
        }
        //12. 檢查綠界扣款結果：1 代表成功，其餘代表失敗（如卡片刷不過、餘額不足）
        if (!"1".equals(callbackParams.get("RtnCode"))) {
            return false;
        }

        // 13. 核對口袋標籤，確認這是租車押金的錢，不是二手車或其他的錢
        if (!CUSTOM_FIELD_CATEGORY.equals(callbackParams.get("CustomField1"))) {
            throw new IllegalArgumentException("付款類型不是租車押金");
        }

        /*

        當客人在綠界輸入完卡號、驗證成功後，綠界的伺服器會發送一個 POST 請求到你的 DEFAULT_RETURN_URL（即 ngrok 網址）。
        這一步是後端的「驗鈔機」，
        必須重新計算並核對檢查碼，防止有人用網頁工具偽造扣款成功的訊號來騙車。
        
        */


        // 解鎖資料庫，更新狀態
        // 14. 從原封不動回傳的口袋裡，把當初藏的訂單 ID 和付款方式挖出來


        Integer orderId = Integer.valueOf(callbackParams.get("CustomField2"));
        PaymentMethod paymentMethod = PaymentMethod.fromValue(callbackParams.get("CustomField3"));
        if (paymentMethod == null) {
            paymentMethod = PaymentMethod.CREDIT_CARD;
        }

        // 15. 去資料庫把那張還在等待的訂單撈出來

        RentalOrderBean order = rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("找不到租車訂單 ID: " + orderId));

        // 16. 雙重核對金額：看綠界真的扣到的錢（TradeAmt），跟資料庫的押金有沒有誤差！
        validateCallbackAmount(callbackParams, order);

        // 17. 冪等性（Idempotent）防護：如果這筆單已經被前一次通知改過狀態了，就直接回傳成功，不重複做
        if (order.getPayStatus() == PayStatus.DEPOSIT_PAID || order.getPayStatus() == PayStatus.PAID) {
            return true;
        }

        // 18. 正式更新資料庫訂單狀態為「已付訂金」，並寫入客人刷卡的工具
        order.setPayStatus(PayStatus.DEPOSIT_PAID);
        order.setDepositPayMethod(paymentMethod);
        RentalOrderBean savedOrder = rentalOrderRepository.save(order);
        applicationEventPublisher.publishEvent(new RentalOrderSuccessMailEvent(savedOrder.getOrderId()));
        return true;
    }

    // 後端利用特製表單與防偽簽章（MacValue）將客戶安全的送往綠界；刷卡成功後，
    // 後端化身驗鈔機核對綠界傳回的小口袋資料，完成資料庫狀態的自動回寫。

    private boolean isValidCheckMacValue(Map<String, String> callbackParams) {
        String receivedCheckMacValue = callbackParams.get("CheckMacValue");
        if (!hasText(receivedCheckMacValue)) return false;

        Map<String, String> paramsForVerify = new HashMap<>(callbackParams);
        paramsForVerify.remove("CheckMacValue");

        String expectedCheckMacValue = EcpayUtil.generateCheckMacValue(
                paramsForVerify,
                ecpayConfig.getPaymentHashKey(),
                ecpayConfig.getPaymentHashIv()
        );

        return receivedCheckMacValue.equalsIgnoreCase(expectedCheckMacValue);
    }

    private void validateCallbackAmount(Map<String, String> callbackParams, RentalOrderBean order) {
        String tradeAmountText = callbackParams.get("TradeAmt");
        if (!hasText(tradeAmountText)) return;

        BigDecimal expectedAmount = order.getDeposit().setScale(0, RoundingMode.HALF_UP);
        BigDecimal actualAmount = new BigDecimal(tradeAmountText).setScale(0, RoundingMode.HALF_UP);
        if (expectedAmount.compareTo(actualAmount) != 0) {
            throw new IllegalArgumentException("綠界付款金額與訂單押金不一致");
        }
    }

    private String buildMerchantTradeNo(Integer orderId) {
        String rawTradeNo = "R" + orderId + System.currentTimeMillis();
        return rawTradeNo.length() <= 20 ? rawTradeNo : rawTradeNo.substring(0, 20);
    }

    private String toEcpayChoosePayment(PaymentMethod paymentMethod) {
        if (paymentMethod == PaymentMethod.CREDIT_CARD) return "Credit";
        if (paymentMethod == PaymentMethod.TRANSFER) return "ATM";
        if (paymentMethod == PaymentMethod.MOBILE_PAY) return "ALL";
        return "ALL";
    }

    private String sanitizeEcpayText(String value) {
        return String.valueOf(value == null ? "" : value).replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9 ]", "");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
