package ute.fit.financemanagement.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ute.fit.financemanagement.enums.OtpPurpose;
import ute.fit.financemanagement.exception.ApiException;

/**
 * OTP lưu trong bộ nhớ (đủ cho phạm vi khoá luận, mất khi restart). Gửi qua Gmail khi đã cấu hình
 * MAIL_USERNAME/MAIL_PASSWORD; nếu chưa thì mã chỉ ghi vào log (đặt OTP_FIXED_CODE để dev dùng mã cố định).
 */
@Slf4j
@Service
public class OtpService {

    private record Entry(String code, Instant expiresAt) {
    }

    private final Map<String, Entry> pending = new ConcurrentHashMap<>();
    private final Map<String, Instant> verified = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final String fixedCode;
    private final Duration ttl;
    private final MailService mailService;

    public OtpService(@Value("${app.otp.fixed-code:}") String fixedCode,
                      @Value("${app.otp.ttl-minutes:5}") long ttlMinutes,
                      MailService mailService) {
        this.fixedCode = fixedCode == null ? "" : fixedCode.trim();
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.mailService = mailService;
    }

    public void send(String email, OtpPurpose purpose) {
        boolean mailOn = mailService.isEnabled();
        // Đã gửi mail thật thì luôn dùng mã ngẫu nhiên; mã cố định chỉ để dev khi chưa có mail.
        String code = (!mailOn && !fixedCode.isEmpty())
                ? fixedCode : String.format("%06d", random.nextInt(1_000_000));
        if (mailOn) {
            mailService.sendOtp(email, code, purpose, ttl.toMinutes());
            log.info("[OTP] Đã gửi mã {} tới email {}", purpose, email);
        } else {
            log.info("[OTP] (chưa cấu hình mail) {} cho {}: {} (hết hạn sau {} phút)",
                    purpose, email, code, ttl.toMinutes());
        }
        pending.put(key(email, purpose), new Entry(code, Instant.now().plus(ttl)));
        verified.remove(key(email, purpose));
    }

    public void verify(String email, OtpPurpose purpose, String otp) {
        String key = key(email, purpose);
        Entry entry = pending.get(key);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            pending.remove(key);
            throw ApiException.badRequest("Mã OTP đã hết hạn, vui lòng gửi lại mã");
        }
        if (!entry.code().equals(otp.trim())) {
            throw ApiException.badRequest("Mã OTP không đúng");
        }
        pending.remove(key);
        verified.put(key, Instant.now().plus(Duration.ofMinutes(10)));
    }

    /** Dùng đúng 1 lần: kiểm tra email đã xác thực OTP gần đây rồi xoá cờ. */
    public void consumeVerified(String email, OtpPurpose purpose) {
        Instant until = verified.remove(key(email, purpose));
        if (until == null || Instant.now().isAfter(until)) {
            throw ApiException.forbidden("Chưa xác thực OTP hoặc phiên xác thực đã hết hạn");
        }
    }

    private String key(String email, OtpPurpose purpose) {
        return purpose + "|" + email.trim().toLowerCase();
    }
}