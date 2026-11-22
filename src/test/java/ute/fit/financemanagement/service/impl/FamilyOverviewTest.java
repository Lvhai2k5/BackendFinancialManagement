package ute.fit.financemanagement.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ute.fit.financemanagement.entity.Family;
import ute.fit.financemanagement.entity.FinancialCategory;
import ute.fit.financemanagement.entity.Membership;
import ute.fit.financemanagement.entity.Person;
import ute.fit.financemanagement.entity.Transaction;
import ute.fit.financemanagement.enums.CategoryStatus;
import ute.fit.financemanagement.enums.TransactionStatus;
import ute.fit.financemanagement.enums.TransactionType;
import ute.fit.financemanagement.exception.ApiException;
import ute.fit.financemanagement.repository.AccountRepository;
import ute.fit.financemanagement.repository.FamilyRepository;
import ute.fit.financemanagement.repository.FinancialCategoryRepository;
import ute.fit.financemanagement.repository.PersonRepository;
import ute.fit.financemanagement.repository.TransactionRepository;
import ute.fit.financemanagement.service.FamilyAccess;

@ExtendWith(MockitoExtension.class)
class FamilyOverviewTest {

    @Mock private FamilyRepository familyRepository;
    @Mock private FinancialCategoryRepository categoryRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private PersonRepository personRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private FamilyAccess access;

    private FamilyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FamilyServiceImpl(familyRepository, categoryRepository, transactionRepository,
                personRepository, accountRepository, access);
    }

    @Test
    void returnsLifetimeApprovedTotalsCategoryUsageAndTenRecentApprovedTransactions() {
        Family family = mock(Family.class);
        Membership membership = mock(Membership.class);
        FinancialCategory category = mock(FinancialCategory.class);
        Transaction transaction = mock(Transaction.class);
        Person person = mock(Person.class);
        when(access.requireFamily(7L)).thenReturn(family);
        when(access.requireMember(family, 3L)).thenReturn(membership);
        when(access.canRead(membership)).thenReturn(true);
        when(categoryRepository.findByFamilyIdOrderByFinancialCategoryId(7L)).thenReturn(List.of(category));
        when(category.getFinancialCategoryId()).thenReturn(11L);
        when(category.getInitialBalance()).thenReturn(new BigDecimal("5000000"));
        when(category.getName()).thenReturn("Giáo dục");
        when(category.getCategoryStatus()).thenReturn(CategoryStatus.ACTIVE);
        when(category.getAmountOfWarning()).thenReturn(new BigDecimal("4000000"));
        when(category.getAmountOfEmergency()).thenReturn(new BigDecimal("5000000"));
        when(transactionRepository.sumAmount(List.of(11L), TransactionType.INCOME,
                TransactionStatus.APPROVED)).thenReturn(new BigDecimal("1000000"));
        when(transactionRepository.sumAmount(List.of(11L), TransactionType.EXPENSE,
                TransactionStatus.APPROVED)).thenReturn(new BigDecimal("1200000"));
        when(transactionRepository.findTop10ByFinancialCategoryIdInAndTransactionStatusOrderByCreatedAtDescTransactionIdDesc(
                List.of(11L), TransactionStatus.APPROVED)).thenReturn(List.of(transaction));
        when(transaction.getCreatedPersonId()).thenReturn(3L);
        when(transaction.getFinancialCategoryId()).thenReturn(11L);
        when(transaction.getTransactionId()).thenReturn(21L);
        when(transaction.getContent()).thenReturn("Sách giáo khoa");
        when(transaction.getAmount()).thenReturn(new BigDecimal("1200000"));
        when(transaction.getTransactionType()).thenReturn(TransactionType.EXPENSE);
        when(transaction.getTransactionStatus()).thenReturn(TransactionStatus.APPROVED);
        when(personRepository.findAllById(List.of(3L))).thenReturn(List.of(person));
        when(person.getPersonId()).thenReturn(3L);
        when(person.getName()).thenReturn("An");

        var result = service.overview(7L, 3L);

        assertThat(result.balance()).isEqualByComparingTo("4800000");
        assertThat(result.income()).isEqualByComparingTo("1000000");
        assertThat(result.expense()).isEqualByComparingTo("1200000");
        assertThat(result.categories()).singleElement().satisfies(progress -> {
            assertThat(progress.spent()).isEqualByComparingTo("1200000");
            assertThat(progress.limitAmount()).isEqualByComparingTo("5000000");
            assertThat(progress.usagePercent()).isEqualByComparingTo("24.00");
            assertThat(progress.allocationPercent()).isEqualTo(100);
            assertThat(progress.amountOfWarning()).isEqualByComparingTo("4000000");
            assertThat(progress.amountOfEmergency()).isEqualByComparingTo("5000000");
        });
        assertThat(result.recentTransactions()).hasSize(1);
        assertThat(result.recentTransactions().getFirst().categoryName()).isEqualTo("Giáo dục");
    }

    @Test
    void rejectsMemberWithoutReadPermissionBeforeLoadingFinancialData() {
        Family family = mock(Family.class);
        Membership membership = mock(Membership.class);
        when(access.requireFamily(7L)).thenReturn(family);
        when(access.requireMember(family, 3L)).thenReturn(membership);
        when(access.canRead(membership)).thenReturn(false);

        assertThatThrownBy(() -> service.overview(7L, 3L))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(categoryRepository, transactionRepository, personRepository);
    }
}
