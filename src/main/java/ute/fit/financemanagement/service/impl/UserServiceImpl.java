package ute.fit.financemanagement.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final PersonRepository personRepository;
    private final AccountRepository accountRepository;
    private final SupportTicketRepository ticketRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(Long personId) {
        return toResponse(requirePerson(personId));
    }

    @Override
    @Transactional
    public UserResponse updateMe(Long personId, UserRequests.UpdateProfile request) {
        Person person = requirePerson(personId);
        personRepository.findByEmailIgnoreCase(request.email())
                .filter(other -> !other.getPersonId().equals(personId))
                .ifPresent(other -> {
                    throw ApiException.conflict("Email đã được sử dụng");
                });
        person.updateProfile(request.name(), request.phone(), request.email());
        return toResponse(personRepository.save(person));
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
                p.getName(), p.getEmail(), p.getPhone(), p.getAvatar(),
                account == null ? null : account.getAccountRole().name());
    }

    private SupportTicketResponse toResponse(SupportTicket t) {
        return new SupportTicketResponse(t.getSupportTicketId(), t.getContent(),
                t.getTicketStatus().name(), t.getNoteOfRespondent());
    }
}