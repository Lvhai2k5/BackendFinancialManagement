package ute.fit.financemanagement.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
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
import ute.fit.financemanagement.service.VoiceParseService;

/**
 * Hình thức nhập Voice (BUSINESS-REQUIREMENTS 3.4.1): transcript → gợi ý điền form. Service này
 * KHÔNG tạo giao dịch và không đổi trạng thái; người dùng xác nhận ở form rồi mới gọi POST /transactions.
 */
@Service
@RequiredArgsConstructor
public class VoiceParseServiceImpl implements VoiceParseService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999999");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "transactionType", Map.of("type", "STRING", "enum", List.of("EXPENSE", "INCOME")),
                    "amount", Map.of("type", "NUMBER", "nullable", true),
                    "content", Map.of("type", "STRING", "nullable", true),
                    "categoryId", Map.of("type", "INTEGER", "nullable", true)),
            "required", List.of("transactionType", "amount", "content", "categoryId"));

    private final FinancialCategoryRepository categoryRepository;
    private final FamilyAccess access;
    private final GeminiClient gemini;

    @Override
    @Transactional(readOnly = true)
    public VoiceResponses.Parsed parse(VoiceRequests.Parse request, Long personId) {
        Family family = access.requireFamily(request.familyId());
        Membership membership = access.requireMember(family, personId);
        if (!access.canWrite(membership)) {
            throw ApiException.forbidden("Bạn không có quyền ghi giao dịch trong gia đình này");
        }
        List<FinancialCategory> categories = categoryRepository
                .findByFamilyIdOrderByFinancialCategoryId(family.getFamilyId()).stream()
                .filter(c -> c.getCategoryStatus() == CategoryStatus.ACTIVE)
                .toList();

        String json = gemini.generateJson(buildPrompt(categories), request.transcript().trim(), SCHEMA);
        return toParsed(json, categories);
    }

    private static String buildPrompt(List<FinancialCategory> categories) {
        String categoryList = categories.isEmpty()
                ? "(chưa có danh mục)"
                : categories.stream()
                        .map(c -> "- id=" + c.getFinancialCategoryId() + ": " + c.getName())
                        .collect(Collectors.joining("\n"));
        return """
                Bạn trích xuất một giao dịch thu/chi từ câu nói tiếng Việt về chi tiêu gia đình.
                Hôm nay là %s.
                Quy tắc:
                - transactionType: EXPENSE nếu là chi/mua/trả/đóng/đổ; INCOME nếu là thu/nhận/lương/thưởng/bán. Mặc định EXPENSE.
                - amount: số tiền VND dạng số dương (không đơn vị). "50k" hoặc "năm chục nghìn" = 50000; "hai triệu rưỡi" = 2500000; "một củ" = 1000000. Không nói số tiền thì null. Không tự đoán.
                - content: mô tả ngắn gọn đúng nội dung được nói (ví dụ "Cà phê"); không có thì null.
                - categoryId: CHỈ chọn id trong danh sách dưới đây, hợp nghĩa nhất. Không chắc chắn thì null. Không bịa id.
                Danh mục:
                %s
                """.formatted(LocalDate.now(ZONE), categoryList);
    }

    /** Không tin LLM: kiểm tra lại số tiền, loại giao dịch và categoryId thuộc danh sách của gia đình. */
    private static VoiceResponses.Parsed toParsed(String json, List<FinancialCategory> categories) {
        JsonNode node;
        try {
            node = JSON.readTree(json);
        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Không phân tích được câu nói, vui lòng nhập thủ công.");
        }
        TransactionType type = "INCOME".equals(node.path("transactionType").asString(""))
                ? TransactionType.INCOME
                : TransactionType.EXPENSE;

        BigDecimal amount = null;
        JsonNode amountNode = node.path("amount");
        if (amountNode.isNumber()) {
            BigDecimal value = amountNode.decimalValue();
            if (value.signum() > 0 && value.compareTo(MAX_AMOUNT) <= 0) {
                amount = value;
            }
        }

        String content = node.path("content").isString() ? node.path("content").asString("").trim() : "";

        FinancialCategory category = null;
        JsonNode categoryNode = node.path("categoryId");
        if (categoryNode.isIntegralNumber()) {
            long id = categoryNode.asLong();
            category = categories.stream()
                    .filter(c -> c.getFinancialCategoryId() == id)
                    .findFirst()
                    .orElse(null);
        }
        return new VoiceResponses.Parsed(
                type,
                amount,
                content.isEmpty() ? null : content,
                category == null ? null : category.getFinancialCategoryId(),
                category == null ? null : category.getName());
    }
}
