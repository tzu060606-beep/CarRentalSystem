package com.carrental.system.rentalorder.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carrental.system.rentalorder.enums.PaymentMethod;
import com.carrental.system.rentalorder.service.RentalEcpayService;

@RestController
@RequestMapping("/api/rental/payment/ecpay")
@CrossOrigin(origins = "*")
public class RentalPaymentController {

    @Autowired
    private RentalEcpayService rentalEcpayService;

    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckout(@RequestBody RentalDepositCheckoutRequest request) {
        try {
            if (request == null || request.getOrderId() == null) {
                return ResponseEntity.badRequest().body(error("請提供租車訂單 ID"));
            }

            PaymentMethod paymentMethod = PaymentMethod.fromValue(request.getPaymentMethod());
            if (hasText(request.getPaymentMethod()) && paymentMethod == null) {
                return ResponseEntity.badRequest().body(error("不支援的押金付款方式"));
            }

            Map<String, Object> checkout = rentalEcpayService.createRentalDepositCheckout(
                    request.getOrderId(),
                    paymentMethod,
                    request.getReturnUrl(),
                    request.getClientBackUrl()
            );

            return ResponseEntity.ok(checkout);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(e.getMessage()));
        }
    }

    @PostMapping("/callback")
    public String paymentCallback(@RequestParam Map<String, String> callbackParams) {
        try {
            boolean paid = rentalEcpayService.handleRentalDepositCallback(callbackParams);
            return paid ? "1|OK" : "0|PaymentFailed";
        } catch (Exception e) {
            return "0|Error";
        }
    }

    private Map<String, String> error(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static class RentalDepositCheckoutRequest {
        private Integer orderId;
        private String paymentMethod;
        private String returnUrl;
        private String clientBackUrl;

        public Integer getOrderId() {
            return orderId;
        }

        public void setOrderId(Integer orderId) {
            this.orderId = orderId;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public String getReturnUrl() {
            return returnUrl;
        }

        public void setReturnUrl(String returnUrl) {
            this.returnUrl = returnUrl;
        }

        public String getClientBackUrl() {
            return clientBackUrl;
        }

        public void setClientBackUrl(String clientBackUrl) {
            this.clientBackUrl = clientBackUrl;
        }
    }
}
