package com.carrental.system.usedCar.EcPay;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.codec.digest.DigestUtils;

public class EcpayUtil {
    
    // 生成綠界專用的 CheckMacValue
    public static String generateCheckMacValue(Map<String, String> params, String key, String iv) {
        // 1. 參數字典排序 (A-Z)
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        
        // 2. 拼接觸發字串
        StringBuilder sb = new StringBuilder("HashKey=" + key);
        sortedParams.forEach((k, v) -> sb.append("&").append(k).append("=").append(v));
        sb.append("&HashIV=" + iv);
        
        // 3. 執行 URL Encode 並替換為綠界指定字元 (文件第 28 點邏輯)
        String encoded = ecpayUrlEncode(sb.toString());
        
        // 4. SHA256 加密並轉大寫
        return DigestUtils.sha256Hex(encoded).toUpperCase();
    }

    private static String ecpayUrlEncode(String data) {
        String encoded = URLEncoder.encode(data, StandardCharsets.UTF_8).toLowerCase();
        return encoded.replace("%2d", "-").replace("%5f", "_").replace("%2e", ".")
                      .replace("%21", "!").replace("%2a", "*").replace("%28", "(")
                      .replace("%29", ")").replace("%20", "+");
    }
}