package com.carrental.system.rentalorder.mail;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.carrental.system.login.entity.CustomerBean;
import com.carrental.system.rentalorder.entity.RentalOrderBean;
import com.carrental.system.vehicle.entity.Location;
import com.carrental.system.vehicle.entity.Vehicle;

import jakarta.mail.internet.MimeMessage;

@Service
public class RentalOrderMailService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public RentalOrderMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOrderSuccessMail(RentalOrderBean order) {
        if (order == null || order.getCustomer() == null) {
            return;
        }

        CustomerBean customer = order.getCustomer();
        String toEmail = safe(customer.getCustEmail());
        if (toEmail.isBlank()) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("OneRent 租車訂單成立通知 #" + order.getOrderId());
            helper.setText(buildOrderSuccessHtml(order), true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("租車訂單成功信寄送失敗：" + e.getMessage(), e);
        }
    }

    private String buildOrderSuccessHtml(RentalOrderBean order) {
        CustomerBean customer = order.getCustomer();
        Vehicle vehicle = order.getVehicle();

        String customerName = customer == null ? "貴賓" : fallback(customer.getCustName(), "貴賓");
        String vehicleName = getVehicleName(vehicle);
        String pickupLocation = getLocationName(order.getPickupLocation());
        String returnLocation = getLocationName(order.getReturnLocation());

        return """
            <div style="font-family:'Noto Sans TC',Arial,sans-serif;max-width:640px;margin:0 auto;padding:24px;background:#f7fbff;color:#102033;">
              <div style="background:#ffffff;border-radius:14px;padding:28px;border:1px solid #dbe8f7;">
                <h2 style="margin:0 0 12px;color:#0057b8;">OneRent 租車訂單成立</h2>
                <p style="margin:0 0 20px;line-height:1.8;">%s 您好，您的租車訂單已建立成功，以下是本次預約資訊。</p>
                <table style="width:100%%;border-collapse:collapse;font-size:15px;">
                  %s
                  %s
                  %s
                  %s
                  %s
                  %s
                  %s
                </table>
                <p style="margin:24px 0 0;color:#607089;font-size:13px;line-height:1.7;">您可以登入會員中心查看訂單狀態。若資料有誤，請盡快聯繫 OneRent 服務人員。</p>
              </div>
            </div>
            """.formatted(
                escapeHtml(customerName),
                row("訂單編號", "#" + order.getOrderId()),
                row("車款", vehicleName),
                row("取車時間", formatDateTime(order.getPickupTime())),
                row("還車時間", formatDateTime(order.getReturnTime())),
                row("取車地點", pickupLocation),
                row("還車地點", returnLocation),
                row("應付押金", formatCurrency(order.getDeposit()))
            );
    }

    private String row(String label, String value) {
        return """
            <tr>
              <td style="width:120px;padding:10px 0;color:#5b6b80;border-top:1px solid #edf2f8;">%s</td>
              <td style="padding:10px 0;font-weight:700;border-top:1px solid #edf2f8;">%s</td>
            </tr>
            """.formatted(escapeHtml(label), escapeHtml(value));
    }

    private String getVehicleName(Vehicle vehicle) {
        if (vehicle == null) {
            return "未指定";
        }
        String modelName = vehicle.getCarModel() == null ? "" : safe(vehicle.getCarModel().getModelName());
        String plateNo = safe(vehicle.getPlateNo());
        String result = (modelName + " " + (plateNo.isBlank() ? "" : "(" + plateNo + ")")).trim();
        return result.isBlank() ? "未指定" : result;
    }

    private String getLocationName(Location location) {
        return location == null ? "未指定" : fallback(location.getLocationName(), "未指定");
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "未指定" : value.format(DATE_TIME_FORMATTER);
    }

    private String formatCurrency(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.TAIWAN);
        formatter.setMaximumFractionDigits(0);
        return formatter.format(amount);
    }

    private String fallback(String value, String fallback) {
        String text = safe(value);
        return text.isBlank() ? fallback : text;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String escapeHtml(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
