package ute.fit.financemanagement.service.impl;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ute.fit.financemanagement.exception.ApiException;
import ute.fit.financemanagement.service.GoogleTokenVerifier;

/**
 * Xác minh ID token bằng endpoint `tokeninfo` của Google: Google tự kiểm tra chữ ký và hạn dùng,
 * backend kiểm tra thêm `aud` (đúng Client ID của ứng dụng), `iss` và `email_verified`.
 */
@Service
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";
    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final String INVALID_TOKEN = "Mã xác thực Google không hợp lệ hoặc đã hết hạn.";

    private final String clientId;
    private final RestClient restClient;

    public GoogleTokenVerifierImpl(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        if (clientId.isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Đăng nhập bằng Google chưa được cấu hình trên máy chủ.");
        }
        Map<String, Object> claims = fetchClaims(idToken);
        return validateClaims(claims);
    }

    private Map<String, Object> fetchClaims(String idToken) {
        try {
            Map<String, Object> body = restClient.get()
                    .uri(TOKENINFO_URL + "?id_token={token}", idToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (body == null) {
                throw ApiException.unauthorized(INVALID_TOKEN);
            }
            return body;
        } catch (HttpClientErrorException e) {
            // Google trả 400 khi token sai chữ ký / hết hạn / sai định dạng.
            throw ApiException.unauthorized(INVALID_TOKEN);
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Không kết nối được tới Google để xác minh. Thử lại sau.");
        }
    }

    /** Tách riêng để test được không cần mạng. */
    GoogleIdentity validateClaims(Map<String, Object> claims) {
        if (!clientId.equals(str(claims.get("aud")))) {
            throw ApiException.unauthorized(INVALID_TOKEN);
        }
        if (!ISSUERS.contains(str(claims.get("iss")))) {
            throw ApiException.unauthorized(INVALID_TOKEN);
        }
        // Google trả email_verified dạng chuỗi "true" (tokeninfo) hoặc boolean.
        if (!"true".equalsIgnoreCase(str(claims.get("email_verified")))) {
            throw ApiException.unauthorized("Email Google chưa được xác minh.");
        }
        String email = str(claims.get("email"));
        if (email.isBlank()) {
            throw ApiException.unauthorized(INVALID_TOKEN);
        }
        return new GoogleIdentity(email.trim(), str(claims.get("name")));
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
