package com.carrental.system.login.security; // 建議統一放在一個 config 包下

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;

import com.carrental.system.usedCar.CalendarApi.OAuth2LoginSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    // 將兩個需要的 Filter 和 Handler 透過建構子注入
    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults()) // 啟用下方的 CORS 設定
            .csrf(csrf -> csrf.disable())    // 關閉 CSRF
            
            // 移除 STATELESS 設定：因為 OAuth2 流程需要短暫使用 Session 來儲存狀態
            // .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) 

            .authorizeHttpRequests(auth -> auth
                // 1. 全域 OPTIONS 放行 (CORS 需要)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // 2. 靜態資源與前端頁面放行 (加入 /uploads/** 讓圖片可以被前台讀取)

                .requestMatchers("/", "/index.html", "/static/**", "/uploads/**").permitAll()
                
                // 3. 登入、登出與 OAuth2 相關路徑放行
                .requestMatchers("/api/auth/employee/login", "/api/auth/employee/logout", 
                                 "/api/auth/customer/login", "/api/auth/customer/logout", 
                                 "/api/auth/customer/register", "/api/auth/customer/google",
                                 "/api/auth/customer/forgot-password", "/api/auth/customer/reset-password",
                                 "/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers(
                    "/api/rental/payment/ecpay/checkout",
                    "/api/rental/payment/ecpay/callback"
                ).permitAll()

                
                //新增:會員首頁租車系統前台需要放行的東西(by偉豪)
                .requestMatchers(HttpMethod.GET,
                     "/api/location",
                     "/api/admin/rentalplans/all",
                     "/api/vehicle/available",
                     "/api/carmodel"
                 ).permitAll()
                
                // 4. 需要登入才能操作的特定 API (例如同步 Google 日曆)
                .requestMatchers("/api/appointments/*/sync-google").authenticated()
                
                // 5. 其他所有 API 預設都要經過 JWT 驗證
                .anyRequest().authenticated()
            )
            
            // 整合 JWT Filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            
            // 整合 Google OAuth2
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2LoginSuccessHandler)
            )
            
            // 整合例外處理 (未授權時的行為)
            .exceptionHandling(e -> e.authenticationEntryPoint((request, response, authException) -> {
                if (request.getRequestURI().startsWith("/api/")) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("UNAUTHORIZED");
                } else {
                    response.sendRedirect("/oauth2/authorization/google");
                }
            }));

        return http.build();
    }

    // 將原本在程式二的 CORS 規則搬過來
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173")); // Vue 預設 Port
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH","OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}