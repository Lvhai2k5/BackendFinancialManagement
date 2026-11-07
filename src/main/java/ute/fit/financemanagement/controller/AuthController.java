package ute.fit.financemanagement.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequests.Login request, HttpServletRequest http) {
        return authService.login(request, http.getRemoteAddr());
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody AuthRequests.Register request,
                                                    HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, http.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody AuthRequests.Refresh request) {
        return authService.refresh(request.refreshToken());
    }

    /** JWT stateless: logout chỉ có tác dụng ở client (xoá token), server trả 204. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/otp/send")
    public MessageResponse sendOtp(@Valid @RequestBody AuthRequests.OtpSend request) {
        return authService.sendOtp(request);
    }

    @PostMapping("/otp/verify")
    public MessageResponse verifyOtp(@Valid @RequestBody AuthRequests.OtpVerify request) {
        return authService.verifyOtp(request);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody AuthRequests.ForgotPassword request) {
        return authService.forgotPassword(request.email());
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody AuthRequests.ResetPassword request) {
        return authService.resetPassword(request);
    }
}