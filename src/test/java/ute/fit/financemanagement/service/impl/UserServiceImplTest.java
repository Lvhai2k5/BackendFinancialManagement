package ute.fit.financemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ute.fit.financemanagement.dto.request.UserRequests;
import ute.fit.financemanagement.entity.Account;
import ute.fit.financemanagement.entity.Person;
import ute.fit.financemanagement.enums.AccountRole;
import ute.fit.financemanagement.exception.ApiException;
import ute.fit.financemanagement.repository.AccountRepository;
import ute.fit.financemanagement.repository.PersonRepository;
import ute.fit.financemanagement.repository.SupportTicketRepository;
import ute.fit.financemanagement.service.AvatarStorage;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock PersonRepository personRepository;
    @Mock AccountRepository accountRepository;
    @Mock SupportTicketRepository ticketRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AvatarStorage avatarStorage;
    @InjectMocks UserServiceImpl service;

    private Person person;

    @BeforeEach
    void setUp() {
        person = Person.builder().personId(8L).name("Before").email("user@example.com")
                .phone("0900000000").build();
        lenient().when(personRepository.findById(8L)).thenReturn(Optional.of(person));
        lenient().when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(accountRepository.findByPersonId(8L)).thenReturn(Optional.empty());
    }

    @Test
    void profileUpdateKeepsEmailAndRejectsDifferentEmail() {
        service.updateMe(8L, new UserRequests.UpdateProfile("After", "0911111111", null));
        assertEquals("user@example.com", person.getEmail());
        assertEquals("After", person.getName());
        assertThrows(ApiException.class, () -> service.updateMe(8L,
                new UserRequests.UpdateProfile("After", "0911111111", "other@example.com")));
        verify(personRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void passwordChangeRequiresCurrentPasswordThenEncodesNewPassword() {
        Account account = Account.builder().accountId(3L).personId(8L).username("user")
                .password("old-hash").accountRole(AccountRole.USER).build();
        when(accountRepository.findByPersonId(8L)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("old", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        service.changePassword(8L, new UserRequests.ChangePassword("old", "new-password"));

        assertEquals("new-hash", account.getPassword());
        verify(accountRepository).save(account);
    }

    @Test
    void passwordChangeRejectsWrongCurrentPassword() {
        Account account = Account.builder().accountId(3L).personId(8L).username("user")
                .password("old-hash").accountRole(AccountRole.USER).build();
        when(accountRepository.findByPersonId(8L)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThrows(ApiException.class, () -> service.changePassword(8L,
                new UserRequests.ChangePassword("wrong", "new-password")));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void avatarRejectsOversizeBeforeStorage() {
        assertThrows(ApiException.class, () -> service.uploadAvatar(8L,
                new byte[5 * 1024 * 1024 + 1]));
        verify(avatarStorage, never()).store(any(), any());
    }
}
