package com.carrental.system.rentalorder.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentMethod {
	
	CREDIT_CARD("信用卡"),
    TRANSFER("轉帳"),
    CASH("現金"),
    MOBILE_PAY("行動支付");

    private final String description;

    //在 Java 的 Enum 裡面，建構子（Constructor）預設就是 private 的
    PaymentMethod(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() { 
    	return description; 
    }

    @JsonCreator
    public static PaymentMethod fromValue(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        //如果前端傳了空字串 ""，或是只傳了幾個空白鍵 "   "，trim() 會把前後空白去掉，isEmpty() 會檢查長度是否為零。
        for (PaymentMethod method : PaymentMethod.values()) {
            // Java Enum 內建的方法，會把你定義的所有 Enum 常數（例如 CASH, CREDIT_CARD）打包成一個陣列。
            if (method.name().equalsIgnoreCase(value) || method.description.equals(value)) {
                // method.name() 會取得 Enum 的英文變數名稱（例如 "CREDIT_CARD"）。
                // equalsIgnoreCase() 會拿它跟前端傳來的字串比對，而且不分大小寫。
                //method.description 是你在 Enum 裡面自己定義的中文屬性（例如 "信用卡"）。
                // equals() 檢查它是否與前端傳來的字串一模一樣。
                return method;
            }
        }
        return null;
    }

}
