package ute.fit.financemanagement.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ute.fit.financemanagement.dto.request.AuthRequests;
import ute.fit.financemanagement.dto.response.AuthResponse;
import ute.fit.financemanagement.dto.response.MessageResponse;
import ute.fit.financemanagement.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập bằng tài khoản và mật khẩu")
    public AuthResponse login(@Valid @RequestBody AuthRequests.Login request, HttpServletRequest http) {
        return authService.login(request, http.getRemoteAddr());
    }

    @PostMapping("/google")
    @Operation(summary = "Đăng nhập bằng Google ID token")
    public AuthResponse loginWithGoogle(@Valid @RequestBody AuthRequests.GoogleLogin request,
                                        HttpServletRequest http) {
        return authService.loginWithGoogle(request, http.getRemoteAddr());
    }

    @PostMapping("/register")
    @Operation(summary = "Tạo tài khoản và gửi OTP đăng ký")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody AuthRequests.Register request,
                                                    HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, http.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Đổi refresh token lấy cặp token mới")
    public AuthResponse refresh(@Valid @RequestBody AuthRequests.Refresh request) {
        return authService.refresh(request.refreshToken());
    }

    /** JWT stateless: logout chỉ có tác dụng ở client (xoá token), server trả 204. */
    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất phía client; JWT stateless không bị thu hồi tại server")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/otp/send")
    @Operation(summary = "Gửi OTP theo email và mục đích")
    public MessageResponse sendOtp(@Valid @RequestBody AuthRequests.OtpSend request) {
        return authService.sendOtp(request);
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Xác minh OTP")
    public MessageResponse verifyOtp(@Valid @RequestBody AuthRequests.OtpVerify request) {
        return authService.verifyOtp(request);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Yêu cầu OTP để đặt lại mật khẩu")
    public MessageResponse forgotPassword(@Valid @RequestBody AuthRequests.ForgotPassword request) {
        return authService.forgotPassword(request.email());
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Đặt mật khẩu mới sau khi xác minh OTP")
    public MessageResponse resetPassword(@Valid @RequestBody AuthRequests.ResetPassword request) {
        return authService.resetPassword(request);
    }
}
