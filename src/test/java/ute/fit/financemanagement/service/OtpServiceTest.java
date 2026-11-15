package ute.fit.financemanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import ute.fit.financemanagement.enums.OtpPurpose;
import ute.fit.financemanagement.exception.ApiException;

class OtpServiceTest {

    private final MailService mailService = mock(MailService.class);
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        when(mailService.isEnabled()).thenReturn(true);
        otpService = new OtpService("", 5, 60, 5, mailService);
    }

    @Test
    void resendDuringCooldownIsRateLimited() {
        otpService.send("test@example.com", OtpPurpose.REGISTER);

        ApiException error = assertThrows(ApiException.class,
                () -> otpService.send("test@example.com", OtpPurpose.REGISTER));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, error.getStatus());
    }

    @Test
    void fifthIncorrectCodeInvalidatesOtp() {
        String email = "test@example.com";
        otpService.send(email, OtpPurpose.REGISTER);
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendOtp(eq(email), codeCaptor.capture(), eq(OtpPurpose.REGISTER), eq(5L));
        String wrongCode = codeCaptor.getValue() + "x";

        for (int attempt = 1; attempt < 5; attempt++) {
            ApiException error = assertThrows(ApiException.class,
                    () -> otpService.verify(email, OtpPurpose.REGISTER, wrongCode));
            assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        }

        ApiException locked = assertThrows(ApiException.class,
                () -> otpService.verify(email, OtpPurpose.REGISTER, wrongCode));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, locked.getStatus());

        ApiException invalidated = assertThrows(ApiException.class,
                () -> otpService.verify(email, OtpPurpose.REGISTER, codeCaptor.getValue()));
        assertEquals(HttpStatus.BAD_REQUEST, invalidated.getStatus());
    }
}
