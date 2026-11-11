package ute.fit.financemanagement.service;

import ute.fit.financemanagement.dto.request.AuthRequests;
import ute.fit.financemanagement.dto.response.AuthResponse;
import ute.fit.financemanagement.dto.response.MessageResponse;

public interface AuthService {

    AuthResponse login(AuthRequests.Login request, String ipAddress);

    /** Đăng nhập bằng Google: liên kết theo email Google trùng với email của tài khoản. */
    AuthResponse loginWithGoogle(AuthRequests.GoogleLogin request, String ipAddress);

    MessageResponse register(AuthRequests.Register request, String ipAddress);

    AuthResponse refresh(String refreshToken);

    MessageResponse sendOtp(AuthRequests.OtpSend request);

    MessageResponse verifyOtp(AuthRequests.OtpVerify request);

    MessageResponse forgotPassword(String email);

    MessageResponse resetPassword(AuthRequests.ResetPassword request);
}