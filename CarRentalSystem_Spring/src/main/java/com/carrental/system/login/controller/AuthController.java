package com.carrental.system.login.controller;



import com.carrental.system.login.dto.EmployeeResponseDTO;
import com.carrental.system.login.entity.EmployeeBean;
import com.carrental.system.login.repository.EmployeeRepository;
import com.carrental.system.login.dto.CustomerResponseDTO;
import com.carrental.system.login.dto.CustomerRequestDTO;
import com.carrental.system.login.entity.CustomerBean;
import com.carrental.system.login.repository.CustomerRepository;
import com.carrental.system.login.service.CustomerService;
import com.carrental.system.login.security.JwtUtil;

import org.springframework.security.core.Authentication; 
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final EmployeeRepository employeeRepository;
    private final JwtUtil jwtUtil; 
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    public AuthController(EmployeeRepository employeeRepository, JwtUtil jwtUtil,
                          CustomerRepository customerRepository, CustomerService customerService) {
        this.employeeRepository = employeeRepository;
        this.jwtUtil = jwtUtil;
        this.customerRepository = customerRepository;
        this.customerService = customerService;
    }

    private EmployeeResponseDTO convertToDto(EmployeeBean entity) {
        if (entity == null) return null;
        EmployeeResponseDTO dto = new EmployeeResponseDTO();
        dto.setEmpId(entity.getEmpId());
        dto.setEmpName(entity.getEmpName());
        dto.setEmpAccount(entity.getEmpAccount());
        dto.setEmpRole(entity.getEmpRole());
        dto.setEmpPhone(entity.getEmpPhone());
        dto.setEmpEmail(entity.getEmpEmail());
        dto.setEmpStatus(entity.getEmpStatus());
        dto.setEmpPhoto(entity.getEmpPhoto());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    // ========== 員工登入 API==========
    @PostMapping("/employee/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> loginRequest) { // 接收前端資料

        String account = loginRequest.get("account");
        String password = loginRequest.get("password");

        // 使用明文密碼查詢
        Optional<EmployeeBean> employeeOpt = employeeRepository.findByEmpAccountAndEmpPassword(account, password);

        Map<String, Object> result = new HashMap<>();

        if (employeeOpt.isPresent()) {
            EmployeeBean employee = employeeOpt.get();
            
            //登入成功
            String token = jwtUtil.generateToken(employee.getEmpId(), employee.getEmpRole());

            result.put("success", true);
            result.put("message", "登入成功");
            result.put("token", token); 
            result.put("role", employee.getEmpRole());
            // 回傳 DTO 以隱藏密碼
            result.put("employee", convertToDto(employee));
            return ResponseEntity.ok(result);
            
        } else { 
            // 找不到該帳號或密碼錯誤
            result.put("success", false);
            result.put("message", "帳號或密碼錯誤！");
            return ResponseEntity.status(401).body(result);
        }
    }

    // ========== 員工登出API ==========
    @PostMapping("/employee/logout")
    public ResponseEntity<Map<String, Object>> logout() { 
        // 因為沒有 Session ，後端只要單純回傳成功即可，不用做任何清除，前端會負責把手上的 Token 丟掉
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已登出");
        return ResponseEntity.ok(result);
    }

    // ========== 客戶登入 API==========
    @PostMapping("/customer/login")
    public ResponseEntity<Map<String, Object>> customerLogin(
            @RequestBody Map<String, String> loginRequest) { 

        String accountOrEmail = loginRequest.get("account");
        String password = loginRequest.get("password");

        // 在 Repository 寫好的方法，同時比對帳號與 Email
        Optional<CustomerBean> customerOpt = customerRepository.findByAccountOrEmailAndPassword(accountOrEmail, password);

        Map<String, Object> result = new HashMap<>();

        if (customerOpt.isPresent()) {
            CustomerBean customer = customerOpt.get();
            
            // 檢查是否被停權
            if ("停權".equals(customer.getCustStatus())) {
                result.put("success", false);
                result.put("message", "此帳號已被停權，請聯絡客服人員！");
                return ResponseEntity.status(401).body(result);
            }
            
            //登入成功，核發專屬角色 ROLE_CUSTOMER
            String token = jwtUtil.generateToken(customer.getCustId(), "CUSTOMER");

            result.put("success", true);
            result.put("message", "登入成功");
            result.put("token", token); 
            // 回傳 DTO 以隱藏密碼
            result.put("customer", customerService.toDto(customer));
            return ResponseEntity.ok(result);
            
        } else { 
            // 找不到該帳號或密碼錯誤
            result.put("success", false);
            result.put("message", "帳號或密碼錯誤！");
            return ResponseEntity.status(401).body(result);
        }
    }

    // ========== 客戶登出API ==========
    @PostMapping("/customer/logout")
    public ResponseEntity<Map<String, Object>> customerLogout() { 
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已登出");
        return ResponseEntity.ok(result);
    }

    // ========== 客戶註冊 API ==========
    @PostMapping("/customer/register")
    public ResponseEntity<Map<String, Object>> customerRegister(@RequestBody CustomerRequestDTO requestDto) {
        Map<String, Object> result = new HashMap<>();

        if (requestDto.getCustPassword() == null || requestDto.getCustPassword().isEmpty()) {
            requestDto.setCustPassword(requestDto.getCustPhone());
        }

        boolean success = customerService.registerCustomer(requestDto);

        if (success) {
            result.put("success", true);
            result.put("message", "註冊成功！");
            return ResponseEntity.ok(result);
        } else {
            result.put("success", false);
            result.put("message", "此帳號或信箱已存在，請更換！");
            return ResponseEntity.badRequest().body(result);
        }
    }

    // ========== 忘記密碼 API (發送重設信件) ==========
    @PostMapping("/customer/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> payload) {
        Map<String, Object> result = new HashMap<>();
        try {
            customerService.forgotPassword(payload.get("email"));
            result.put("success", true);
            result.put("message", "重設密碼信件已寄出，請至信箱查收！");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    // ========== 重設密碼 API (驗證 Token 並更新密碼) ==========
    @PostMapping("/customer/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> payload) {
        Map<String, Object> result = new HashMap<>();
        try {
            customerService.resetPassword(payload.get("token"), payload.get("newPassword"));
            result.put("success", true);
            result.put("message", "密碼重設成功！請使用新密碼登入。");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    // ========== 客戶 Google 登入 API ==========
    @PostMapping("/customer/google")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody Map<String, String> payload) {
        Map<String, Object> result = new HashMap<>();
        String credential = payload.get("credential");

        if (credential == null || credential.isEmpty()) {
            result.put("success", false);
            result.put("message", "Google 登入失敗：未提供憑證");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken != null) {
                GoogleIdToken.Payload tokenPayload = idToken.getPayload();
                String email = tokenPayload.getEmail();
                String name = (String) tokenPayload.get("name");

                // 呼叫 Service 執行登入或註冊邏輯
                CustomerResponseDTO customer = customerService.loginOrRegisterWithGoogle(email, name);

             // 檢查如果手機號碼，如果是空值，代表這是一位需要去補全資料的新使用者
             boolean isNewUser = (customer.getCustPhone() == null || customer.getCustPhone().trim().isEmpty());
                
                // 產生 JWT Token
                String token = jwtUtil.generateToken(
                        customer.getCustId(),
                        "CUSTOMER"
                );

                result.put("success", true);
                result.put("message", "Google 登入成功！");
                result.put("token", token);
                result.put("isNewUser", isNewUser);//是否新用戶?
                result.put("customer", customer); // 回傳完整客戶資料
                return ResponseEntity.ok(result);
            } else {
                result.put("success", false);
                result.put("message", "Google 登入失敗：憑證無效");
                return ResponseEntity.status(401).body(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Google 登入處理異常：" + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    // ========== 檢查登入狀態API ==========
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkLogin() {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> result = new HashMap<>();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) { 
            // 老師將 JwtAuthenticationFilter 裡的 principal 改成了 LoggedInUserDTO
            com.carrental.system.login.dto.LoggedInUserDTO userDTO = (com.carrental.system.login.dto.LoggedInUserDTO) auth.getPrincipal();
            
            result.put("loggedIn", true);
            result.put("role", userDTO.getRole());
            
            // 根據不同的身分 (員工或客戶) 查詢對應的資料表來回傳資訊
            if (userDTO.getTable() == com.carrental.system.login.dto.LoggedInUserDTO.TableType.EMPLOYEE) {
                employeeRepository.findById(userDTO.getId()).ifPresent(emp -> {
                    result.put("employee", convertToDto(emp));
                    result.put("account", emp.getEmpAccount());
                });
            } else if (userDTO.getTable() == com.carrental.system.login.dto.LoggedInUserDTO.TableType.CUSTOMER) {
                customerRepository.findById(userDTO.getId()).ifPresent(cust -> {
                    result.put("customer", customerService.toDto(cust));
                    result.put("account", cust.getCustAccount());
                });
            }
            
        } else {
            result.put("loggedIn", false);
        }
        return ResponseEntity.ok(result);
    }
    
    
    
    
}
