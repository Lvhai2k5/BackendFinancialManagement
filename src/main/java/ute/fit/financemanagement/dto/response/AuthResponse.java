package ute.fit.financemanagement.dto.response;

/** Cặp token — khớp `AuthResponse` của Mobile (accessToken/refreshToken). */
public record AuthResponse(String accessToken, String refreshToken) {
}