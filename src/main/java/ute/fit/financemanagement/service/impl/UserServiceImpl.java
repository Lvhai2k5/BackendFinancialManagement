package ute.fit.financemanagement.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ute.fit.financemanagement.dto.request.UserRequests;
import ute.fit.financemanagement.dto.response.SupportTicketResponse;
import ute.fit.financemanagement.dto.response.UserResponse;
import ute.fit.financemanagement.entity.Account;
import ute.fit.financemanagement.entity.Person;
import ute.fit.financemanagement.entity.SupportTicket;
import ute.fit.financemanagement.enums.TicketStatus;
import ute.fit.financemanagement.exception.ApiException;
import ute.fit.financemanagement.repository.AccountRepository;
import ute.fit.financemanagement.repository.PersonRepository;
import ute.fit.financemanagement.repository.SupportTicketRepository;
import ute.fit.financemanagement.service.UserService;
import ute.fit.financemanagement.service.AvatarStorage;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final PersonRepository personRepository;
    private final AccountRepository accountRepository;
    private final SupportTicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorage avatarStorage;

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(Long personId) {
        return toResponse(requirePerson(personId));
    }

    @Override
    @Transactional
    public UserResponse updateMe(Long personId, UserRequests.UpdateProfile request) {
        Person person = requirePerson(personId);
        if (request.email() != null && !request.email().isBlank()
                && !request.email().trim().equalsIgnoreCase(person.getEmail())) {
            throw ApiException.badRequest("Không thể thay đổi email tại đây");
        }
        person.updateProfile(request.name(), request.phone());
        return toResponse(personRepository.save(person));
    }

    @Override
    @Transactional
    public UserResponse uploadAvatar(Long personId, byte[] content) {
        if (content == null || content.length == 0) throw ApiException.badRequest("Ảnh tải lên đang trống");
        if (content.length > 5 * 1024 * 1024) throw ApiException.payloadTooLarge("Ảnh tối đa 5 MiB");
        String detectedType = detectImageType(content);
        if (detectedType == null) throw ApiException.badRequest("Tệp tải lên không phải ảnh hợp lệ");
        Person person = requirePerson(personId);
        person.updateAvatar(avatarStorage.store(content, personId));
        return toResponse(personRepository.save(person));
    }

    @Override
    @Transactional
    public void changePassword(Long personId, UserRequests.ChangePassword request) {
        Account account = accountRepository.findByPersonId(personId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản"));
        if (!passwordEncoder.matches(request.currentPassword(), account.getPassword())) {
            throw ApiException.badRequest("Mật khẩu hiện tại không chính xác");
        }
        if (passwordEncoder.matches(request.newPassword(), account.getPassword())) {
            throw ApiException.badRequest("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        account.changePassword(passwordEncoder.encode(request.newPassword()));
        accountRepository.save(account);
    }

    private String detectImageType(byte[] data) {
        if (data.length >= 3 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xd8
                && (data[2] & 0xff) == 0xff) return "image/jpeg";
        if (data.length >= 8 && (data[0] & 0xff) == 0x89 && data[1] == 0x50
                && data[2] == 0x4e && data[3] == 0x47) return "image/png";
        if (data.length >= 12 && new String(data, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                && new String(data, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")) return "image/webp";
        if (data.length >= 6) {
            String signature = new String(data, 0, 6, java.nio.charset.StandardCharsets.US_ASCII);
            if (signature.equals("GIF87a") || signature.equals("GIF89a")) return "image/gif";
        }
        if (data.length >= 12
                && new String(data, 4, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("ftyp")) {
            String brand = new String(data, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if (brand.startsWith("hei") || brand.equals("mif1") || brand.equals("avif")) return "image/heic";
        }
        return null;
    }

    @Override
    @Transactional
    public SupportTicketResponse createTicket(Long personId, UserRequests.CreateSupportTicket request) {
        SupportTicket ticket = ticketRepository.save(SupportTicket.builder()
                .content(request.content().trim())
                .createdPersonId(personId)
                .ticketStatus(TicketStatus.SUBMITTED)
                .build());
        return toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketResponse> myTickets(Long personId) {
        return ticketRepository.findByCreatedPersonIdOrderBySupportTicketIdDesc(personId).stream()
                .map(this::toResponse).toList();
    }

    private Person requirePerson(Long personId) {
        return personRepository.findById(personId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy người dùng"));
    }

    private UserResponse toResponse(Person p) {
        Account account = accountRepository.findByPersonId(p.getPersonId()).orElse(null);
        return new UserResponse(p.getPersonId(), account == null ? null : account.getUsername(),
                p.getName(), p.getEmail(), p.getPhone(), avatarStorage.url(p.getAvatar()),
                account == null ? null : account.getAccountRole().name());
    }

    private SupportTicketResponse toResponse(SupportTicket t) {
        return new SupportTicketResponse(t.getSupportTicketId(), t.getContent(),
                t.getTicketStatus().name(), t.getNoteOfRespondent(), t.getCreatedAt());
    }
}
