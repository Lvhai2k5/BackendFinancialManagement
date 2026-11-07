package ute.fit.financemanagement.service;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import ute.fit.financemanagement.enums.OtpPurpose;
import ute.fit.financemanagement.exception.ApiException;

/**
 * Gửi email OTP qua Gmail SMTP. Chỉ hoạt động khi đã đặt MAIL_USERNAME/MAIL_PASSWORD
 * (App Password của Gmail); nếu chưa thì {@link #isEnabled()} = false và OtpService chỉ ghi log.
 */
@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String username;
    private final String fromName;

    public MailService(JavaMailSender mailSender,
                       @Value("${spring.mail.username:}") String username,
                       @Value("${app.mail.from-name:FinNest}") String fromName) {
        this.mailSender = mailSender;
        this.username = username == null ? "" : username.trim();
        this.fromName = fromName;
    }

    public boolean isEnabled() {
        return !username.isEmpty();
    }

    public void sendOtp(String toEmail, String code, OtpPurpose purpose, long ttlMinutes) {
        String action = purpose == OtpPurpose.REGISTER ? "xác thực đăng ký tài khoản" : "đặt lại mật khẩu";
        String subject = "[" + fromName + "] Mã OTP " + action;
        String html = """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:auto;padding:24px;\
                background:#fff8f5;border-radius:16px">
                  <h2 style="color:#95473b;margin:0 0 8px">%s</h2>
                  <p style="color:#544340">Mã OTP để %s của bạn là:</p>
                  <p style="font-size:34px;letter-spacing:8px;font-weight:700;color:#201b18;margin:16px 0">%s</p>
                  <p style="color:#544340">Mã có hiệu lực trong <b>%d phút</b>. Tuyệt đối không chia sẻ mã này cho bất kỳ ai.</p>
                  <p style="color:#87726f;font-size:12px">Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.</p>
                </div>
                """.formatted(fromName, action, code, ttlMinutes);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(username, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText("Mã OTP " + action + " của bạn là: " + code + " (hiệu lực " + ttlMinutes + " phút).",
                    html);
            mailSender.send(message);
            log.info("[Mail] Đã gửi OTP {} tới {}", purpose, toEmail);
        } catch (Exception e) {
            log.error("[Mail] Gửi OTP tới {} thất bại: {}", toEmail, e.getMessage());
            throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Không gửi được email OTP, vui lòng thử lại sau.");
        }
    }
}