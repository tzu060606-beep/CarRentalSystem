package com.carrental.system.usedCar.EcPay;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EcpayConfig {

    // 讀取金流配置
    @Value("${ecpay.payment.merchant-id}")
    private String paymentMerchantId;

    @Value("${ecpay.payment.hash-key}")
    private String paymentHashKey;

    @Value("${ecpay.payment.hash-iv}")
    private String paymentHashIv;

    @Value("${ecpay.payment.url}")
    private String paymentUrl;

    // Getters ... (建議產生 Getter 方便 Service 使用)
    public String getPaymentMerchantId() { return paymentMerchantId; }
    public String getPaymentHashKey() { return paymentHashKey; }
    public String getPaymentHashIv() { return paymentHashIv; }
    public String getPaymentUrl() { return paymentUrl; }
}
