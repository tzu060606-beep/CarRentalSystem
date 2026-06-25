package com.carrental.system.login.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carrental.system.login.dto.CustomerRequestDTO;
import com.carrental.system.login.dto.CustomerResponseDTO;
import com.carrental.system.login.entity.CustomerBean;
import com.carrental.system.login.repository.CustomerRepository;
import com.carrental.system.point.enums.ChangeType;
import com.carrental.system.point.service.PointsEventService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PointsEventService pointsEventService;
    private final JavaMailSender mailSender;

    public CustomerService(CustomerRepository customerRepository,
            @Lazy PointsEventService pointsEventService,
            JavaMailSender mailSender) {
this.customerRepository = customerRepository;
this.pointsEventService = pointsEventService;
this.mailSender = mailSender;
    }

    // ========== DTO 轉換邏輯 ==========
    public CustomerResponseDTO toDto(CustomerBean entity) {
        if (entity == null) return null;
        CustomerResponseDTO dto = new CustomerResponseDTO();
        dto.setCustId(entity.getCustId());
        dto.setCustName(entity.getCustName());
        dto.setCustPhone(entity.getCustPhone());
        dto.setCustEmail(entity.getCustEmail());
        dto.setCustAccount(entity.getCustAccount());
        dto.setCustAddress(entity.getCustAddress());
        dto.setCustLicense(entity.getCustLicense());
        dto.setCustLicenseExpiry(entity.getCustLicenseExpiry());
        dto.setCustStatus(entity.getCustStatus());
        dto.setCurrentPoints(entity.getCurrentPoints());
        dto.setTotalAccumulated(entity.getTotalAccumulated());
        dto.setCustAvatar(entity.getCustAvatar());
        return dto;
    }

    // ========== 註冊新客戶 ==========
    public boolean registerCustomer(CustomerRequestDTO requestDto) {
        if (customerRepository.existsByCustAccount(requestDto.getCustAccount())) {
            return false;
        }
        CustomerBean customer = new CustomerBean();
        customer.setCustName(requestDto.getCustName());
        customer.setCustPhone(requestDto.getCustPhone());
        customer.setCustEmail(requestDto.getCustEmail());
        customer.setCustAccount(requestDto.getCustAccount());
        customer.setCustPassword(requestDto.getCustPassword());
        customer.setCustAddress(requestDto.getCustAddress());
        customer.setCustLicense(requestDto.getCustLicense());
        customer.setCustLicenseExpiry(requestDto.getCustLicenseExpiry());
        customer.setCustStatus(requestDto.getCustStatus());
        // 預設點數為 0
        customer.setCurrentPoints(0);
        customer.setTotalAccumulated(0);
        CustomerBean savedCustomer = customerRepository.save(customer);
        pointsEventService.addFixedPoints(savedCustomer.getCustId(), ChangeType.WELCOME_BONUS);
        return true;
    }

    // ========== 查詢全部客戶 ==========
    public List<CustomerResponseDTO> getAllCustomers() {
        return customerRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    // ========== 根據 ID 查詢單一客戶 ==========
    public Optional<CustomerResponseDTO> getCustomerById(Integer custId) {
        return customerRepository.findById(custId).map(this::toDto);
    }

    // ========== 模糊查詢 ==========
    public List<CustomerResponseDTO> searchByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllCustomers();
        }
        return customerRepository.searchByKeyword(keyword).stream().map(this::toDto).collect(Collectors.toList());
    }

    // ========== 修改客戶 ==========
    public CustomerResponseDTO updateCustomer(Integer custId, CustomerRequestDTO updatedData) {
        return customerRepository.findById(custId)
                .map(existing -> {
                    existing.setCustName(updatedData.getCustName());
                    existing.setCustPhone(updatedData.getCustPhone());
                    existing.setCustEmail(updatedData.getCustEmail());
                    existing.setCustAccount(updatedData.getCustAccount());
                    existing.setCustAddress(updatedData.getCustAddress());
                    existing.setCustLicense(updatedData.getCustLicense());
                    existing.setCustLicenseExpiry(updatedData.getCustLicenseExpiry());
                    existing.setCustStatus(updatedData.getCustStatus());
//點數通常管理員不能更改   existing.setCurrentPoints(updatedData.getCurrentPoints());
//點數通常管理員不能更改   existing.setTotalAccumulated(updatedData.getTotalAccumulated());
                    
                    if (updatedData.getCustPassword() != null && !updatedData.getCustPassword().trim().isEmpty()) {
                        existing.setCustPassword(updatedData.getCustPassword());
                    }

                    if (updatedData.getCustAvatar() == null || updatedData.getCustAvatar().trim().isEmpty()) {
                        existing.setCustAvatar(null);
                    }

                    if (updatedData.getAvatarBase64() != null && !updatedData.getAvatarBase64().trim().isEmpty()) {
                        try {
                            String base64Data = updatedData.getAvatarBase64();
                            // 去除 Base64 的前綴 (例如 data:image/png;base64,)
                            if (base64Data.contains(",")) {
                                base64Data = base64Data.split(",")[1];
                            }
                            byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Data);
                            String fileName = UUID.randomUUID().toString() + ".png"; 
                            java.io.File directory = new java.io.File("uploads/avatars");
                            if (!directory.exists()) {
                                directory.mkdirs();
                            }
                            java.nio.file.Path destinationFile = java.nio.file.Paths.get("uploads/avatars", fileName);
                            java.nio.file.Files.write(destinationFile, decodedBytes);
                            // 儲存 URL 路徑
                            existing.setCustAvatar("http://localhost:8081/uploads/avatars/" + fileName);
                        } catch (Exception e) {
                            throw new RuntimeException("圖片儲存失敗：" + e.getMessage());
                        }
                    }
                    
                    return toDto(customerRepository.save(existing));
                })
                .orElseThrow(() -> new RuntimeException("找不到客戶 ID: " + custId));
    }

    // ========== 軟刪除客戶（改為停權，不真正刪除資料）==========
    public void deleteCustomer(Integer custId) {
        customerRepository.findById(custId).ifPresent(existing -> {
            existing.setCustStatus("停權"); // 符合資料庫 CHECK 約束
            customerRepository.save(existing);
        });
    }
    
    // ========== 更新客戶點數（供 F3 點數模組呼叫）==========
    public void updatePoints(Integer custId, int pointsDelta) {
        CustomerBean customer = customerRepository.findById(custId)
            .orElseThrow(() -> new RuntimeException("找不到客戶 ID: " + custId));
        customer.setCurrentPoints(customer.getCurrentPoints() + pointsDelta);//給當前點數
        customer.setTotalAccumulated(customer.getTotalAccumulated() + Math.max(0, pointsDelta)); //給歷史點數:只有增加點數時，才累積歷史點數
        customerRepository.save(customer);
    }

    // ========== 取得當前登入客戶的完整資訊 (供其他組員呼叫) ==========
    public CustomerResponseDTO getCurrentCustomer() {
        // 1. 從 Spring Security 總部拿出當前登入者的「統一識別證」
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (principal.equals("anonymousUser")) {
            throw new RuntimeException("使用者未登入！");
        }

        com.carrental.system.login.dto.LoggedInUserDTO loggedInUser = (com.carrental.system.login.dto.LoggedInUserDTO) principal;
        
        // 2. 檢查這個登入的人是不是「客戶」
        if (loggedInUser.getTable() != com.carrental.system.login.dto.LoggedInUserDTO.TableType.CUSTOMER) {
            throw new RuntimeException("目前登入的不是客戶帳號，無法取得客戶資料！");
        }
        
    // 3. 拿著識別證上的 ID，去資料庫把這個客戶的完整資料撈出來並回傳
        return getCustomerById(loggedInUser.getId())
                .orElseThrow(() -> new RuntimeException("資料庫中找不到該客戶資料！"));
    }

    // ========== Google 第三方登入 / 自動註冊 ==========
    public CustomerResponseDTO loginOrRegisterWithGoogle(String email, String name) {
        // 1. 檢查此 Email 是否已經註冊過
        Optional<CustomerBean> existingCustomer = customerRepository.findByCustEmail(email);

        CustomerBean customer;
        if (existingCustomer.isPresent()) {
            customer = existingCustomer.get();
            // 檢查是否被停權
            if ("停權".equals(customer.getCustStatus())) {
                throw new RuntimeException("此帳號已被停權，請聯絡客服人員！");
            }
        } else {
            // 2. 若尚未註冊，自動幫他建立帳號
            customer = new CustomerBean();
            customer.setCustEmail(email);
            customer.setCustName(name != null ? name : "Google 使用者");
            // Google 登入者帳號就直接預設為 email 前半部或整段，這裡用 email 當帳號
            customer.setCustAccount(email);
            // 隨機產生一組密碼（因為他以後都用 Google 登入）或者設為空
            customer.setCustPassword("GOOGLE_SSO_USER_" + java.util.UUID.randomUUID().toString());
            customer.setCustStatus("啟用");
            customer.setCurrentPoints(0);
            customer.setTotalAccumulated(0);

            // 若帳號重複，加點後綴避免唯一性衝突
            int counter = 1;
            while (customerRepository.existsByCustAccount(customer.getCustAccount())) {
                customer.setCustAccount(email + "_" + counter);
                counter++;
            }

            customer = customerRepository.save(customer);
            // 贈送註冊點數
            pointsEventService.addFixedPoints(customer.getCustId(), ChangeType.WELCOME_BONUS);
        }

        return toDto(customer);
    }

    // ========== 忘記密碼：產生 Token 並寄送 Email ==========
    public void forgotPassword(String email) {
        Optional<CustomerBean> customerOpt = customerRepository.findByCustEmail(email);
        if (customerOpt.isEmpty()) {
            throw new RuntimeException("此 Email 尚未註冊！");
        }

        CustomerBean customer = customerOpt.get();

        // 產生隨機 Token 並設定 15 分鐘後過期
        String token = UUID.randomUUID().toString();
        customer.setResetToken(token);
        customer.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        customerRepository.save(customer);

        // 寄送重設密碼信件
        String resetLink = "http://localhost:5173/customer/reset-password?token=" + token;
        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("【OneRent 租車】重設您的登入密碼");
            helper.setText(
                "<div style='font-family: sans-serif; max-width: 500px; margin: auto; padding: 20px;'>" +
                "<h2 style='color: #00448a;'>OneRent 租車系統</h2>" +
                "<p>親愛的 <b>" + customer.getCustName() + "</b> 您好，</p>" +
                "<p>我們收到了您的密碼重設請求。請點擊下方按鈕設定新密碼：</p>" +
                "<p style='text-align: center; margin: 30px 0;'>" +
                "<a href='" + resetLink + "' style='background-color: #00448a; color: white; padding: 12px 30px; " +
                "text-decoration: none; border-radius: 5px; font-weight: bold;'>重設密碼</a></p>" +
                "<p style='color: #888; font-size: 13px;'>此連結將於 <b>15 分鐘</b>後失效。如果這不是您本人的操作，請忽略此信件。</p>" +
                "</div>", true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("寄送郵件失敗：" + e.getMessage());
        }
    }

    // ========== 重設密碼：驗證 Token 並更新密碼 ==========
    public void resetPassword(String token, String newPassword) {
        Optional<CustomerBean> customerOpt = customerRepository.findByResetToken(token);
        if (customerOpt.isEmpty()) {
            throw new RuntimeException("此重設連結無效！");
        }

        CustomerBean customer = customerOpt.get();

        // 檢查 Token 是否已過期
        if (customer.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            // 過期了，清掉 token
            customer.setResetToken(null);
            customer.setResetTokenExpiry(null);
            customerRepository.save(customer);
            throw new RuntimeException("此重設連結已過期，請重新申請！");
        }

        // Token 有效，更新密碼並清除 Token
        customer.setCustPassword(newPassword);
        customer.setResetToken(null);
        customer.setResetTokenExpiry(null);
        customerRepository.save(customer);
    }
}
