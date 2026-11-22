package ute.fit.financemanagement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import ute.fit.financemanagement.dto.request.TransactionRequests;
import ute.fit.financemanagement.entity.Account;
import ute.fit.financemanagement.entity.Family;
import ute.fit.financemanagement.entity.FinancialCategory;
import ute.fit.financemanagement.entity.Transaction;
import ute.fit.financemanagement.enums.AccountRole;
import ute.fit.financemanagement.enums.AccountStatus;
import ute.fit.financemanagement.enums.FamilyMemberRole;
import ute.fit.financemanagement.enums.FamilyPermission;
import ute.fit.financemanagement.enums.TransactionStatus;
import ute.fit.financemanagement.enums.TransactionType;
import ute.fit.financemanagement.exception.ApiException;
import ute.fit.financemanagement.repository.AccountRepository;
import ute.fit.financemanagement.repository.FamilyRepository;
import ute.fit.financemanagement.repository.FinancialCategoryRepository;
import ute.fit.financemanagement.repository.PersonRepository;
import ute.fit.financemanagement.repository.TransactionRepository;
import ute.fit.financemanagement.security.JwtAuthenticationFilter;
import ute.fit.financemanagement.security.JwtService;
import ute.fit.financemanagement.service.ActivityAudit;
import ute.fit.financemanagement.service.FamilyAccess;
import ute.fit.financemanagement.service.impl.TransactionServiceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UseCasePermissionsTest {
    private final FamilyRepository families = mock(FamilyRepository.class);
    private final FinancialCategoryRepository categories = mock(FinancialCategoryRepository.class);
    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final FamilyAccess access = new FamilyAccess(families);
    private final TransactionServiceImpl service = new TransactionServiceImpl(transactions, categories, families,
            mock(PersonRepository.class), access, mock(ActivityAudit.class));

    @Test
    void transferKeepsOneOwnerAndRotatesInvitation() {
        Family family = Family.create("Tổ ấm", 1L);
        family.addMember(2L, List.of(FamilyPermission.APPROVAL));
        family.transferOwnership(1L, 2L);

        assertEquals(1, family.getMemberships().stream()
                .filter(m -> m.getFamilyMemberRole() == FamilyMemberRole.OWNER).count());
        assertTrue(access.isOwner(access.requireMember(family, 2L)));
        assertFalse(access.canApprove(access.requireMember(family, 1L)));
        UUID previousInvite = family.getInviteVersion();
        family.removeMember(1L);
        assertNotEquals(previousInvite, family.getInviteVersion());
        assertThrows(IllegalStateException.class, () -> family.removeMember(2L));
    }

    @Test
    void memberWithoutReadCannotListFamilyTransactions() {
        Family family = Family.create("Tổ ấm", 1L);
        family.addMember(2L, List.of(FamilyPermission.WRITE));
        when(families.findById(1L)).thenReturn(Optional.of(family));
        when(families.findAllByMember(2L)).thenReturn(List.of(family));
        when(categories.findByFamilyIdIn(List.of())).thenReturn(List.of());
        assertEquals(403, assertThrows(ApiException.class,
                () -> service.list(2L, 1L, PageRequest.of(0, 20))).getStatus().value());
        assertTrue(service.list(2L, null, PageRequest.of(0, 20)).isEmpty());
        verifyNoInteractions(transactions);
    }

    @Test
    void offlineRetryChecksCurrentWritePermission() {
        Family family = Family.create("Tổ ấm", 1L);
        family.addMember(2L, List.of(FamilyPermission.READ));
        when(categories.findFamilyId(3L)).thenReturn(Optional.of(1L));
        when(families.findForUpdate(1L)).thenReturn(Optional.of(family));
        assertEquals(403, assertThrows(ApiException.class,
                () -> service.create(offlineRequest(UUID.randomUUID()), 2L)).getStatus().value());
        verifyNoInteractions(transactions);
    }

    @Test
    void offlineRetryReturnsPreviousTransactionEvenAfterJarWasLocked() {
        Family family = Family.create("Tổ ấm", 1L);
        family.addMember(2L, List.of(FamilyPermission.WRITE));
        ReflectionTestUtils.setField(family, "familyId", 1L);
        FinancialCategory category = family.createFinancialCategory("Ăn uống", null,
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        ReflectionTestUtils.setField(category, "financialCategoryId", 3L);
        category.lockCategory();
        UUID key = UUID.randomUUID();
        Transaction previous = Transaction.builder().transactionId(4L).financialCategoryId(3L)
                .content("Ăn sáng").amount(BigDecimal.ONE).transactionType(TransactionType.EXPENSE)
                .transactionStatus(TransactionStatus.APPROVED).createdPersonId(2L).clientRequestId(key).build();
        when(categories.findFamilyId(3L)).thenReturn(Optional.of(1L));
        when(categories.findById(3L)).thenReturn(Optional.of(category));
        when(families.findForUpdate(1L)).thenReturn(Optional.of(family));
        when(transactions.findByCreatedPersonIdAndClientRequestId(2L, key)).thenReturn(Optional.of(previous));

        assertEquals(4L, service.create(offlineRequest(key), 2L).transactionId());
        verify(transactions, never()).save(any());
    }

    @Test
    void lockedAccountCannotUsePreviouslyIssuedAccessToken() throws Exception {
        JwtService jwt = new JwtService("test-secret-for-locked-account-regression-only", 15, 1);
        AccountRepository accounts = mock(AccountRepository.class);
        when(accounts.findByPersonId(2L)).thenReturn(Optional.of(Account.builder()
                .personId(2L).accountRole(AccountRole.USER).accountStatus(AccountStatus.INACTIVE).build()));
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt.createAccessToken(2L, AccountRole.USER));
        try {
            new JwtAuthenticationFilter(jwt, accounts).doFilter(request, new MockHttpServletResponse(),
                    (req, res) -> assertNull(SecurityContextHolder.getContext().getAuthentication()));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private TransactionRequests.Create offlineRequest(UUID key) {
        return new TransactionRequests.Create(3L, "Ăn sáng", BigDecimal.ONE,
                TransactionType.EXPENSE, key, null, 2L);
    }
}
