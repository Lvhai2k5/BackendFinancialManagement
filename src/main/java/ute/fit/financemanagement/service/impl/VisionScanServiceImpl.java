package ute.fit.financemanagement.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
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
import ute.fit.financemanagement.service.VisionScanService;

/**
 * Hình thức "Quét đồ vật": ảnh → tên vật phẩm. Giống Voice, service này KHÔNG tạo giao dịch; người dùng
 * chỉ nhập giá ở form rồi mới gọi POST /transactions.
 */
@Service
@RequiredArgsConstructor
public class VisionScanServiceImpl implements VisionScanService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "itemName", Map.of("type", "STRING", "nullable", true),
                    "categoryId", Map.of("type", "INTEGER", "nullable", true)),
            "required", List.of("itemName", "categoryId"));

    private final FinancialCategoryRepository categoryRepository;
    private final FamilyAccess access;
    private final GeminiClient gemini;

    @Override
    @Transactional(readOnly = true)
    public VoiceResponses.Parsed scanItem(VisionRequests.ScanItem request, Long personId) {
        Family family = access.requireFamily(request.familyId());
        Membership membership = access.requireMember(family, personId);
        if (!access.canWrite(membership)) {
            throw ApiException.forbidden("Bạn không có quyền ghi giao dịch trong gia đình này");
        }
        List<FinancialCategory> categories = categoryRepository
                .findByFamilyIdOrderByFinancialCategoryId(family.getFamilyId()).stream()
                .filter(c -> c.getCategoryStatus() == CategoryStatus.ACTIVE)
                .toList();

        String json = gemini.generateJsonFromImage(
                buildPrompt(categories), request.mimeType(), request.imageBase64().trim(), SCHEMA);
        return toParsed(json, categories);
    }

    private static String buildPrompt(List<FinancialCategory> categories) {
        String categoryList = categories.isEmpty()
                ? "(chưa có danh mục)"
                : categories.stream()
                        .map(c -> "- id=" + c.getFinancialCategoryId() + ": " + c.getName())
                        .collect(Collectors.joining("\n"));
        return """
                Bạn nhận diện món đồ chính trong ảnh để người dùng ghi một khoản chi tiêu gia đình.
                Quy tắc:
                - itemName: tên ngắn gọn bằng tiếng Việt của món đồ chính (ví dụ "Ly cà phê", "Sữa tươi", "Áo thun").
                  Ảnh không có đồ vật rõ ràng, quá mờ hoặc không chắc chắn thì null. Không tự bịa.
                - categoryId: CHỈ chọn id trong danh sách dưới đây, hợp nghĩa nhất với món đồ. Không chắc chắn thì null.
                Danh mục:
                %s
                """.formatted(categoryList);
    }

    /** Không tin LLM: cắt độ dài tên và kiểm tra categoryId thuộc danh sách của gia đình. */
    private static VoiceResponses.Parsed toParsed(String json, List<FinancialCategory> categories) {
        JsonNode node;
        try {
            node = JSON.readTree(json);
        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Không nhận diện được đồ vật, vui lòng nhập thủ công.");
        }
        String name = node.path("itemName").isString() ? node.path("itemName").asString("").trim() : "";
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH);
        }
        if (name.isEmpty()) {
            throw ApiException.badRequest(
                    "Không nhận diện được đồ vật trong ảnh, vui lòng chụp rõ hơn hoặc nhập thủ công.");
        }

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
                TransactionType.EXPENSE,
                null,
                name,
                category == null ? null : category.getFinancialCategoryId(),
                category == null ? null : category.getName());
    }
}
