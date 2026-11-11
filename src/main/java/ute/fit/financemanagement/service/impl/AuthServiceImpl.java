package ute.fit.financemanagement.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ute.fit.financemanagement.dto.request.AuthRequests;
import ute.fit.financemanagement.dto.response.AuthResponse;
import ute.fit.financemanagement.dto.response.MessageResponse;
import ute.fit.financemanagement.entity.Account;
import ute.fit.financemanagement.entity.ActivityLog;
import ute.fit.financemanagement.entity.Person;
import ute.fit.financemanagement.enums.AccountRole;
import ute.fit.financemanagement.enums.AccountStatus;
import ute.fit.financemanagement.enums.ActionType;
import ute.fit.financemanagement.enums.ActivityStatus;
import ute.fit.financemanagement.enums.OtpPurpose;
import ute.fit.financemanagement.exception.ApiException;
import ute.fit.financemanagement.repository.AccountRepository;
import ute.fit.financemanagement.repository.ActivityLogRepository;
import ute.fit.financemanagement.repository.PersonRepository;
import ute.fit.financemanagement.security.CurrentUser;
import ute.fit.financemanagement.security.JwtService;
import ute.fit.financemanagement.service.AuthService;
import ute.fit.financemanagement.service.GoogleTokenVerifier;
import ute.fit.financemanagement.service.OtpService;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String BAD_CREDENTIALS = "Tên tài khoản hoặc mật khẩu không đúng.";
    private static final String GOOGLE_NOT_LINKED =
            "Email Google này chưa được liên kết với tài khoản FinNest nào.";

    private final PersonRepository personRepository;
    private final AccountRepository accountRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Override
    @Transactional
    public AuthResponse login(AuthRequests.Login request, String ipAddress) {
        String login = request.username().trim();
        Account account = accountRepository.findByUsernameIgnoreCase(login)
                .or(() -> personRepository.findByEmailIgnoreCase(login)
                        .flatMap(p -> accountRepository.findByPersonId(p.getPersonId())))
                .orElse(null);

        if (account == null || !passwordEncoder.matches(request.password(), account.getPassword())) {
            log(ActionType.LOGIN, ActivityStatus.FAILED, "Đăng nhập thất bại: " + login, ipAddress,
                    account == null ? null : account.getPersonId());
            throw ApiException.unauthorized(BAD_CREDENTIALS);
        }
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw ApiException.forbidden("Tài khoản chưa được kích hoạt hoặc đã bị khoá.");
        }
        log(ActionType.LOGIN, ActivityStatus.SUCCESS, "Đăng nhập thành công", ipAddress, account.getPersonId());
        return tokens(account.getPersonId(), account.getAccountRole());
    }

    /**
     * Không đánh dấu @Transactional: ActivityLog "thất bại" phải được lưu độc lập, nếu không sẽ bị
     * rollback cùng lúc với ApiException ném ra bên dưới.
     */
    @Override
    public AuthResponse loginWithGoogle(AuthRequests.GoogleLogin request, String ipAddress) {
        GoogleTokenVerifier.GoogleIdentity identity = googleTokenVerifier.verify(request.idToken().trim());

        Person person = personRepository.findByEmailIgnoreCase(identity.email()).orElse(null);
        Account account = person == null ? null : accountRepository.findByPersonId(person.getPersonId()).orElse(null);
        if (account == null) {
            log(ActionType.LOGIN, ActivityStatus.FAILED,
                    "Đăng nhập Google thất bại: email chưa liên kết " + identity.email(), ipAddress, null);
            throw ApiException.unauthorized(GOOGLE_NOT_LINKED);
        }
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            log(ActionType.LOGIN, ActivityStatus.FAILED,
                    "Đăng nhập Google bị từ chối: tài khoản không hoạt động", ipAddress, account.getPersonId());
            throw ApiException.forbidden("Tài khoản chưa được kích hoạt hoặc đã bị khoá.");
        }
        log(ActionType.LOGIN, ActivityStatus.SUCCESS, "Đăng nhập Google thành công", ipAddress,
                account.getPersonId());
        return tokens(account.getPersonId(), account.getAccountRole());
    }

    @Override
    @Transactional
    public MessageResponse register(AuthRequests.Register request, String ipAddress) {
        String email = request.resolvedEmail();
        String username = request.username() == null || request.username().isBlank()
                ? email : request.username().trim();

        if (personRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Email đã được sử dụng");
        }
        if (accountRepository.existsByUsernameIgnoreCase(username)) {
            throw ApiException.conflict("Tên tài khoản đã tồn tại");
        }

        Person person = personRepository.save(Person.builder()
                .name(request.name().trim())
                .email(email)
                .phone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim())
                .build());
        // Chưa kích hoạt cho tới khi xác thực OTP đăng ký (Mobile: register -> OTP -> login).
        Account account = Account.builder()
                .personId(person.getPersonId())
                .username(username)
                .password(passwordEncoder.encode(request.password()))
                .accountRole(AccountRole.USER)
                .accountStatus(AccountStatus.INACTIVE)
                .build();
        accountRepository.save(account);
        log(ActionType.REGISTER, ActivityStatus.SUCCESS, "Đăng ký tài khoản " + username, ipAddress,
                person.getPersonId());

        otpService.send(email, OtpPurpose.REGISTER);
        return new MessageResponse("Đã gửi mã OTP xác thực tới " + email);
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        CurrentUser user = jwtService.parseRefresh(refreshToken);
        Account account = accountRepository.findByPersonId(user.personId())
                .orElseThrow(() -> ApiException.unauthorized("Tài khoản không tồn tại"));
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw ApiException.forbidden("Tài khoản đã bị khoá.");
        }
        return tokens(account.getPersonId(), account.getAccountRole());
    }

    @Override
    public MessageResponse sendOtp(AuthRequests.OtpSend request) {
        // Không tiết lộ email có tồn tại hay không: luôn trả thành công.
        if (personRepository.existsByEmailIgnoreCase(request.email())) {
            otpService.send(request.email(), request.purpose());
        }
        return new MessageResponse("Nếu email hợp lệ, mã OTP đã được gửi.");
    }

    @Override
    @Transactional
    public MessageResponse verifyOtp(AuthRequests.OtpVerify request) {
        otpService.verify(request.email(), request.purpose(), request.otp());
        if (request.purpose() == OtpPurpose.REGISTER) {
            Account account = accountByEmail(request.email());
            account.unlockAccount();
            accountRepository.save(account);
            otpService.consumeVerified(request.email(), OtpPurpose.REGISTER);
        }
        return new MessageResponse("Xác thực OTP thành công");
    }

    @Override
    public MessageResponse forgotPassword(String email) {
        if (personRepository.existsByEmailIgnoreCase(email)) {
            otpService.send(email, OtpPurpose.RESET_PASSWORD);
        }
        return new MessageResponse("Nếu email hợp lệ, mã OTP đặt lại mật khẩu đã được gửi.");
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(AuthRequests.ResetPassword request) {
        otpService.consumeVerified(request.email(), OtpPurpose.RESET_PASSWORD);
        Account account = accountByEmail(request.email());
        account.changePassword(passwordEncoder.encode(request.newPassword()));
        accountRepository.save(account);
        return new MessageResponse("Đặt lại mật khẩu thành công");
    }

    private Account accountByEmail(String email) {
        Person person = personRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản"));
        return accountRepository.findByPersonId(person.getPersonId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản"));
    }

    private AuthResponse tokens(Long personId, AccountRole role) {
        return new AuthResponse(jwtService.createAccessToken(personId, role),
                jwtService.createRefreshToken(personId, role));
    }

    private void log(ActionType type, ActivityStatus status, String description, String ip, Long personId) {
        activityLogRepository.save(ActivityLog.builder()
                .actionType(type)
                .activityStatus(status)
                .activityDescription(description)
                .ipAddress(ip)
                .performedPersonId(personId)
                .build());
    }
}