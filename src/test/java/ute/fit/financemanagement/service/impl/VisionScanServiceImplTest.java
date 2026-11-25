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
import ute.fit.financemanagement.dto.request.VisionRequests;
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
class VisionScanServiceImplTest {

    private static final long PERSON_ID = 7L;
    private static final long FAMILY_ID = 1L;
    private static final VisionRequests.ScanItem REQUEST =
            new VisionRequests.ScanItem(FAMILY_ID, "image/jpeg", " QUJD ");

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

    private VisionScanServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VisionScanServiceImpl(categoryRepository, access, gemini);
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
        when(gemini.generateJsonFromImage(anyString(), anyString(), anyString(), any())).thenReturn(json);
    }

    @Test
    void scanItem_recognizedItem_returnsNameWithoutAmount() {
        geminiReturns("{\"itemName\":\"Ly cà phê\",\"categoryId\":10}");

        VoiceResponses.Parsed result = service.scanItem(REQUEST, PERSON_ID);

        assertThat(result.transactionType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(result.amount()).isNull();
        assertThat(result.content()).isEqualTo("Ly cà phê");
        assertThat(result.financialCategoryId()).isEqualTo(10L);
        assertThat(result.categoryName()).isEqualTo("Ăn uống");
        verify(gemini).generateJsonFromImage(anyString(), org.mockito.ArgumentMatchers.eq("image/jpeg"),
                org.mockito.ArgumentMatchers.eq("QUJD"), any());
    }

    @Test
    void scanItem_unknownCategory_leavesCategoryEmpty() {
        geminiReturns("{\"itemName\":\"Áo thun\",\"categoryId\":999}");

        VoiceResponses.Parsed result = service.scanItem(REQUEST, PERSON_ID);

        assertThat(result.content()).isEqualTo("Áo thun");
        assertThat(result.financialCategoryId()).isNull();
        assertThat(result.categoryName()).isNull();
    }

    @Test
    void scanItem_lockedCategory_isNotSuggested() {
        geminiReturns("{\"itemName\":\"Vé máy bay\",\"categoryId\":11}");

        VoiceResponses.Parsed result = service.scanItem(REQUEST, PERSON_ID);

        assertThat(result.financialCategoryId()).isNull();
    }

    @Test
    void scanItem_nothingRecognized_returnsBadRequest() {
        geminiReturns("{\"itemName\":null,\"categoryId\":null}");

        assertThatThrownBy(() -> service.scanItem(REQUEST, PERSON_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void scanItem_malformedJson_returnsBadGateway() {
        geminiReturns("không phải json");

        assertThatThrownBy(() -> service.scanItem(REQUEST, PERSON_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void scanItem_withoutWritePermission_isForbiddenAndSkipsGemini() {
        when(access.canWrite(membership)).thenReturn(false);

        assertThatThrownBy(() -> service.scanItem(REQUEST, PERSON_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(gemini, never()).generateJsonFromImage(anyString(), anyString(), anyString(), any());
    }
}
