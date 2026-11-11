package ute.fit.financemanagement.service;

/** Xác minh ID token (JWT) do Google Identity Services cấp cho client. */
public interface GoogleTokenVerifier {

    /**
     * @return danh tính Google đã được xác minh (chữ ký, audience, hạn dùng, email đã xác thực)
     * @throws ute.fit.financemanagement.exception.ApiException khi token sai/hết hạn hoặc chưa cấu hình Client ID
     */
    GoogleIdentity verify(String idToken);

    /** Thông tin tối thiểu lấy từ ID token đã xác minh. */
    record GoogleIdentity(String email, String name) {
    }
}
