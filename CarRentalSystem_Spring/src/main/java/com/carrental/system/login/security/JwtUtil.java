package com.carrental.system.login.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    // 從環境變數讀取 JWT 密鑰，未設定時使用預設值（僅供本地開發）
    private static final String SECRET = System.getenv("JWT_SECRET") != null
            ? System.getenv("JWT_SECRET")
            : "DefaultDevKeyForLocalOnly_ChangeInProduction123456789";
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET.getBytes()); //把「字串密碼」轉成「JWT 可以用的加密鑰匙（SecretKey）」
    //這個key用來「產生 Signature」
    
    // 這個 token 只能用1 天（246060*1000）
    private static final long EXPIRATION_TIME = 86400000L; 

    /**
     * 
     * 當員工帳號密碼正確時，會呼叫這個方法。
     */
    public String generateToken(Integer id,  String role) {//產生 JWT 字串，要放進 JWT 的資料
        Map<String, Object> claims = new HashMap<>(); //= 放在 Payload 裡的資料= 寫在 JWT 裡的「使用者資訊」
        claims.put("role", role); //把員工 or 客戶的權限寫進去 
       
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(id.toString()) 
                .setIssuedAt(new Date(System.currentTimeMillis())) // 現在時間（毫秒）
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 到期時間
                .signWith(KEY, SignatureAlgorithm.HS256) //使用演算法HMAC + SHA256，產生 JWT 第三段：Signature
                .compact(); // 把所有東西組合起來Header.Payload.Signature並轉成字串
    }

    /**
     * 當前端帶著 Token 來敲門時，我們會用同一把鑰匙 (KEY) 來驗證JWT。
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY) // 拿出key比對
                .build()
                .parseClaimsJws(token) // 如果比對後不對，這行會直接報錯，程式就不會繼續往下走！
                .getBody(); // 驗證成功，把裡面的資訊 (帳號、角色) 拿出來
    }
}
