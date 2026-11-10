package ute.fit.financemanagement.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import ute.fit.financemanagement.dto.request.VoiceRequests;
import ute.fit.financemanagement.dto.response.VoiceResponses;
import ute.fit.financemanagement.entity.Family;
import ute.fit.financemanagement.entity.FinancialCategory;
import ute.fit.financemanagement.entity.Membership;
import ute.fit.financemanagement.enums.CategoryStatus;
import ute.fit.financemanagement.enums.TransactionType;
import ute.fit.financemanagement.exception.ApiException;
import ute.fit.financemanagement.repository.FinancialCategoryRepository;
import ute.fit.financemanagement.service.FamilyAccess;
import ute.fit.financemanagement.service.GeminiClient;

@ExtendWith(MockitoExtension.class)
class VoiceParseServiceImplTest {

    private static final long PERSON_ID = 7L;
    private static final long FAMILY_ID = 1L;
    private static final VoiceRequests.Parse REQUEST = new VoiceRequests.Parse(FAMILY_ID, " chi 50 nghìn cà phê ");

    @Mock
    private FinancialCategoryRepository categoryRepository;
    @Mock
    private FamilyAccess access;
    @Mock
    private GeminiClient gemini;
    @Mock
    private Family family;
    @Mock
    private Membership membership;

    private VoiceParseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VoiceParseServiceImpl(categoryRepository, access, gemini);
        lenient().when(family.getFamilyId()).thenReturn(FAMILY_ID);
        lenient().when(access.requireFamily(FAMILY_ID)).thenReturn(family);
        lenient().when(access.requireMember(family, PERSON_ID)).thenReturn(membership);
        lenient().when(access.canWrite(membership)).thenReturn(true);
        FinancialCategory active = category(10L, "Ăn uống", CategoryStatus.ACTIVE);
        FinancialCategory locked = category(11L, "Du lịch", CategoryStatus.LOCKED);
        lenient().when(categoryRepository.findByFamilyIdOrderByFinancialCategoryId(FAMILY_ID))
                .thenReturn(List.of(active, locked));
    }

    private static FinancialCategory category(long id, String name, CategoryStatus status) {
        FinancialCategory c = mock(FinancialCategory.class);
        lenient().when(c.getFinancialCategoryId()).thenReturn(id);
        lenient().when(c.getName()).thenReturn(name);
        lenient().when(c.getCategoryStatus()).thenReturn(status);
        return c;
    }

    private void geminiReturns(String json) {
        when(gemini.generateJson(anyString(), anyString(), any())).thenReturn(json);
    }

    @Test
    void parse_fullSentence_returnsPrefilledSuggestion() {
        geminiReturns("{\"transactionType\":\"EXPENSE\",\"amount\":50000,\"content\":\"Cà phê\",\"categoryId\":10}");

        VoiceResponses.Parsed result = service.parse(REQUEST, PERSON_ID);

        assertThat(result.transactionType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(result.amount()).isEqualByComparingTo("50000");
        assertThat(result.content()).isEqualTo("Cà phê");
        assertThat(result.financialCategoryId()).isEqualTo(10L);
        assertThat(result.categoryName()).isEqualTo("Ăn uống");
    }

    @Test
    void parse_income_isMapped() {
        geminiReturns("{\"transactionType\":\"INCOME\",\"amount\":15000000,\"content\":\"Lương\",\"categoryId\":null}");

        VoiceResponses.Parsed result = service.parse(REQUEST, PERSON_ID);

        assertThat(result.transactionType()).isEqualTo(TransactionType.INCOME);
        assertThat(result.financialCategoryId()).isNull();
    }

    @Test
    void parse_missingAmount_leavesAmountNull() {
        geminiReturns("{\"transactionType\":\"EXPENSE\",\"amount\":null,\"content\":\"Mua đồ\",\"categoryId\":null}");

        assertThat(service.parse(REQUEST, PERSON_ID).amount()).isNull();
    }

    @Test
    void parse_nonPositiveOrHugeAmount_isDropped() {
        geminiReturns("{\"transactionType\":\"EXPENSE\",\"amount\":-5,\"content\":\"x\",\"categoryId\":null}");
        assertThat(service.parse(REQUEST, PERSON_ID).amount()).isNull();

        geminiReturns("{\"transactionType\":\"EXPENSE\",\"amount\":1e15,\"content\":\"x\",\"categoryId\":null}");
        assertThat(service.parse(REQUEST, PERSON_ID).amount()).isNull();
    }

    @Test
    void parse_categoryNotInFamilyOrLocked_isDropped() {
        geminiReturns("{\"transactionType\":\"EXPENSE\",\"amount\":1000,\"content\":\"x\",\"categoryId\":999}");
        assertThat(service.parse(REQUEST, PERSON_ID).financialCategoryId()).isNull();

        geminiReturns("{\"transactionType\":\"EXPENSE\",\"amount\":1000,\"content\":\"x\",\"categoryId\":11}");
        assertThat(service.parse(REQUEST, PERSON_ID).financialCategoryId()).isNull();
    }

    @Test
    void parse_unknownTypeOrBlankContent_fallsBackSafely() {
        geminiReturns("{\"transactionType\":\"WHATEVER\",\"amount\":1000,\"content\":\"  \",\"categoryId\":null}");

        VoiceResponses.Parsed result = service.parse(REQUEST, PERSON_ID);

        assertThat(result.transactionType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(result.content()).isNull();
    }

    @Test
    void parse_malformedJson_throwsBadGateway() {
        geminiReturns("không phải json");

        assertThatThrownBy(() -> service.parse(REQUEST, PERSON_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void parse_withoutWritePermission_isForbiddenAndNeverCallsGemini() {
        when(access.canWrite(membership)).thenReturn(false);

        assertThatThrownBy(() -> service.parse(REQUEST, PERSON_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(gemini, never()).generateJson(anyString(), anyString(), any());
    }

    @Test
    void parse_promptListsOnlyActiveCategories() {
        geminiReturns("{\"transactionType\":\"EXPENSE\",\"amount\":null,\"content\":null,\"categoryId\":null}");

        service.parse(REQUEST, PERSON_ID);

        verify(gemini).generateJson(
                org.mockito.ArgumentMatchers.argThat(p -> p.contains("id=10: Ăn uống") && !p.contains("Du lịch")),
                org.mockito.ArgumentMatchers.eq("chi 50 nghìn cà phê"),
                any());
    }
}
