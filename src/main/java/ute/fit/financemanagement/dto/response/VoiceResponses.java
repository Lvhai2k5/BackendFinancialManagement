package ute.fit.financemanagement.dto.response;

import java.math.BigDecimal;
import ute.fit.financemanagement.enums.TransactionType;

public final class VoiceResponses {

    private VoiceResponses() {
    }

    /**
     * Gợi ý điền sẵn form giao dịch. Mọi trường (trừ loại) có thể null khi câu nói không đủ thông tin —
     * Mobile để trống cho người dùng tự nhập, không đoán bừa.
     */
    public record Parsed(
            TransactionType transactionType,
            BigDecimal amount,
            String content,
            Long financialCategoryId,
            String categoryName) {
    }
}
