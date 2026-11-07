package ute.fit.financemanagement.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ute.fit.financemanagement.enums.AccountRole;
import ute.fit.financemanagement.exception.ApiException;

/** Tạo/giải mã JWT — access (ngắn hạn) và refresh (dài hạn) theo TECH-AUTH.md. */
@Component
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-minutes}") long accessMinutes,
                      @Value("${app.jwt.refresh-days}") long refreshDays) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET phải dài tối thiểu 32 ký tự");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofMinutes(accessMinutes);
        this.refreshTtl = Duration.ofDays(refreshDays);
    }

    public String createAccessToken(Long personId, AccountRole role) {
        return build(personId, role, TYPE_ACCESS, accessTtl);
    }

    public String createRefreshToken(Long personId, AccountRole role) {
        return build(personId, role, TYPE_REFRESH, refreshTtl);
    }

    /** Trả về người dùng nếu là access token hợp lệ, ngược lại ném 401. */
    public CurrentUser parseAccess(String token) {
        return parse(token, TYPE_ACCESS);
    }

    public CurrentUser parseRefresh(String token) {
        return parse(token, TYPE_REFRESH);
    }

    private String build(Long personId, AccountRole role, String type, Duration ttl) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(personId))
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl.toMillis()))
                .signWith(key)
                .compact();
    }

    private CurrentUser parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw ApiException.unauthorized("Loại token không hợp lệ");
            }
            return new CurrentUser(Long.valueOf(claims.getSubject()),
                    AccountRole.valueOf(claims.get(CLAIM_ROLE, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            throw ApiException.unauthorized("Token không hợp lệ hoặc đã hết hạn");
        }
    }
}
