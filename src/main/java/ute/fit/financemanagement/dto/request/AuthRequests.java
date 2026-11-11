package ute.fit.financemanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ute.fit.financemanagement.enums.OtpPurpose;

/** Payload các endpoint `/api/v1/auth/**` — khớp `Frontend-Mobile/lib/models/auth_model.dart`. */
public final class AuthRequests {

    private AuthRequests() {
    }

    public record Login(
            @NotBlank(message = "Tên tài khoản không được để trống") String username,
            @NotBlank(message = "Mật khẩu không được để trống") String password) {
    }

    public record Register(
            @NotBlank(message = "Họ tên không được để trống") String name,
            /** Mobile gửi `email`, Web gửi `gmail` — nhận cả hai. */
            @Email(message = "Email không đúng định dạng") String email,
            @Email(message = "Email không đúng định dạng") String gmail,
            String phone,
            /** Tuỳ chọn: bỏ trống thì dùng email làm tên đăng nhập. */
            String username,
            @NotBlank(message = "Mật khẩu không được để trống")
            @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự") String password) {

        public String resolvedEmail() {
            String value = email != null && !email.isBlank() ? email : gmail;
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Email không được để trống");
            }
            return value.trim();
        }
    }

    public record GoogleLogin(
            @NotBlank(message = "Thiếu mã xác thực Google") String idToken) {
    }

    public record Refresh(@NotBlank(message = "Thiếu refresh token") String refreshToken) {
    }

    public record OtpSend(
            @NotBlank @Email(message = "Email không đúng định dạng") String email,
            @NotNull(message = "Thiếu mục đích OTP") OtpPurpose purpose) {
    }

    public record OtpVerify(
            @NotBlank @Email(message = "Email không đúng định dạng") String email,
            @NotNull(message = "Thiếu mục đích OTP") OtpPurpose purpose,
            @NotBlank(message = "Mã OTP không được để trống") String otp) {
    }

    public record ForgotPassword(
            @NotBlank @Email(message = "Email không đúng định dạng") String email) {
    }

    public record ResetPassword(
            @NotBlank @Email(message = "Email không đúng định dạng") String email,
            @NotBlank(message = "Mật khẩu mới không được để trống")
            @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự") String newPassword) {
    }
}