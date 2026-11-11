package ute.fit.financemanagement.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import ute.fit.financemanagement.dto.request.AuthRequests;
import ute.fit.financemanagement.dto.response.AuthResponse;
import ute.fit.financemanagement.entity.Account;
import ute.fit.financemanagement.entity.ActivityLog;
import ute.fit.financemanagement.entity.Person;
import ute.fit.financemanagement.enums.AccountRole;
import ute.fit.financemanagement.enums.AccountStatus;
import ute.fit.financemanagement.enums.ActivityStatus;
import ute.fit.financemanagement.exception.ApiException;
import ute.fit.financemanagement.repository.AccountRepository;
import ute.fit.financemanagement.repository.ActivityLogRepository;
import ute.fit.financemanagement.repository.PersonRepository;
import ute.fit.financemanagement.security.JwtService;
import ute.fit.financemanagement.service.GoogleTokenVerifier;
import ute.fit.financemanagement.service.OtpService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplGoogleLoginTest {

    private static final String IP = "127.0.0.1";
    private static final AuthRequests.GoogleLogin REQUEST = new AuthRequests.GoogleLogin(" id-token ");

    @Mock
    private PersonRepository personRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ActivityLogRepository activityLogRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private OtpService otpService;
    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(personRepository, accountRepository, activityLogRepository,
                passwordEncoder, jwtService, otpService, googleTokenVerifier);
    }

    private static Person person(long id, String email) {
        return Person.builder().personId(id).name("An").email(email).build();
    }

    private static Account account(long personId, AccountStatus status) {
        return Account.builder().personId(personId).username("an").password("x")
                .accountRole(AccountRole.USER).accountStatus(status).build();
    }

    @Test
    void logsInWhenGoogleEmailMatchesActiveAccount() {
        when(googleTokenVerifier.verify("id-token"))
                .thenReturn(new GoogleTokenVerifier.GoogleIdentity("an@gmail.com", "An"));
        when(personRepository.findByEmailIgnoreCase("an@gmail.com")).thenReturn(Optional.of(person(7L, "an@gmail.com")));
        when(accountRepository.findByPersonId(7L)).thenReturn(Optional.of(account(7L, AccountStatus.ACTIVE)));
        when(jwtService.createAccessToken(7L, AccountRole.USER)).thenReturn("access");
        when(jwtService.createRefreshToken(7L, AccountRole.USER)).thenReturn("refresh");

        AuthResponse response = service.loginWithGoogle(REQUEST, IP);

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        ArgumentCaptor<ActivityLog> log = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(log.capture());
        assertThat(log.getValue().getActivityStatus()).isEqualTo(ActivityStatus.SUCCESS);
        assertThat(log.getValue().getPerformedPersonId()).isEqualTo(7L);
    }

    @Test
    void rejectsWhenGoogleEmailIsNotLinkedToAnyAccount() {
        when(googleTokenVerifier.verify("id-token"))
                .thenReturn(new GoogleTokenVerifier.GoogleIdentity("lạ@gmail.com", "Lạ"));
        when(personRepository.findByEmailIgnoreCase("lạ@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loginWithGoogle(REQUEST, IP))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        ArgumentCaptor<ActivityLog> log = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(log.capture());
        assertThat(log.getValue().getActivityStatus()).isEqualTo(ActivityStatus.FAILED);
        verify(jwtService, never()).createAccessToken(any(), any());
    }

    @Test
    void rejectsWhenPersonHasNoAccount() {
        when(googleTokenVerifier.verify("id-token"))
                .thenReturn(new GoogleTokenVerifier.GoogleIdentity("an@gmail.com", "An"));
        when(personRepository.findByEmailIgnoreCase("an@gmail.com")).thenReturn(Optional.of(person(7L, "an@gmail.com")));
        when(accountRepository.findByPersonId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loginWithGoogle(REQUEST, IP))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void rejectsLockedAccountEvenWithValidGoogleToken() {
        when(googleTokenVerifier.verify("id-token"))
                .thenReturn(new GoogleTokenVerifier.GoogleIdentity("an@gmail.com", "An"));
        when(personRepository.findByEmailIgnoreCase("an@gmail.com")).thenReturn(Optional.of(person(7L, "an@gmail.com")));
        when(accountRepository.findByPersonId(7L)).thenReturn(Optional.of(account(7L, AccountStatus.INACTIVE)));

        assertThatThrownBy(() -> service.loginWithGoogle(REQUEST, IP))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        ArgumentCaptor<ActivityLog> log = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(log.capture());
        assertThat(log.getValue().getActivityStatus()).isEqualTo(ActivityStatus.FAILED);
        verify(jwtService, never()).createAccessToken(any(), any());
    }

    @Test
    void propagatesInvalidGoogleTokenWithoutTouchingDatabase() {
        when(googleTokenVerifier.verify("id-token")).thenThrow(ApiException.unauthorized("Mã xác thực Google không hợp lệ."));

        assertThatThrownBy(() -> service.loginWithGoogle(REQUEST, IP)).isInstanceOf(ApiException.class);

        verify(personRepository, never()).findByEmailIgnoreCase(any());
        verify(activityLogRepository, never()).save(any());
    }
}
