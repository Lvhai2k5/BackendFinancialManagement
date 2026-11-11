package ute.fit.financemanagement.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import ute.fit.financemanagement.exception.ApiException;
import ute.fit.financemanagement.service.GoogleTokenVerifier;

class GoogleTokenVerifierImplTest {

    private static final String CLIENT_ID = "abc123.apps.googleusercontent.com";

    private final GoogleTokenVerifierImpl verifier = new GoogleTokenVerifierImpl(CLIENT_ID);

    private static Map<String, Object> validClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("aud", CLIENT_ID);
        claims.put("iss", "https://accounts.google.com");
        claims.put("email", "  an@gmail.com ");
        claims.put("email_verified", "true");
        claims.put("name", "Nguyễn An");
        return claims;
    }

    @Test
    void acceptsValidClaimsAndTrimsEmail() {
        GoogleTokenVerifier.GoogleIdentity identity = verifier.validateClaims(validClaims());

        assertThat(identity.email()).isEqualTo("an@gmail.com");
        assertThat(identity.name()).isEqualTo("Nguyễn An");
    }

    @Test
    void acceptsIssuerWithoutScheme() {
        Map<String, Object> claims = validClaims();
        claims.put("iss", "accounts.google.com");

        assertThat(verifier.validateClaims(claims).email()).isEqualTo("an@gmail.com");
    }

    @Test
    void acceptsBooleanEmailVerified() {
        Map<String, Object> claims = validClaims();
        claims.put("email_verified", Boolean.TRUE);

        assertThat(verifier.validateClaims(claims).email()).isEqualTo("an@gmail.com");
    }

    @Test
    void rejectsTokenIssuedForAnotherApp() {
        Map<String, Object> claims = validClaims();
        claims.put("aud", "other-app.apps.googleusercontent.com");

        assertThatThrownBy(() -> verifier.validateClaims(claims))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void rejectsUnknownIssuer() {
        Map<String, Object> claims = validClaims();
        claims.put("iss", "https://evil.example.com");

        assertThatThrownBy(() -> verifier.validateClaims(claims)).isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsUnverifiedEmail() {
        Map<String, Object> claims = validClaims();
        claims.put("email_verified", "false");

        assertThatThrownBy(() -> verifier.validateClaims(claims))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void rejectsMissingEmail() {
        Map<String, Object> claims = validClaims();
        claims.remove("email");

        assertThatThrownBy(() -> verifier.validateClaims(claims)).isInstanceOf(ApiException.class);
    }

    @Test
    void reportsUnavailableWhenClientIdNotConfigured() {
        GoogleTokenVerifierImpl unconfigured = new GoogleTokenVerifierImpl("  ");

        assertThatThrownBy(() -> unconfigured.verify("any-token"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
