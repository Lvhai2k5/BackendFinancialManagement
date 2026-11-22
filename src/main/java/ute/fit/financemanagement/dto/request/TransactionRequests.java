package ute.fit.financemanagement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import ute.fit.financemanagement.enums.TransactionType;

public final class TransactionRequests {

    private TransactionRequests() {
    }

    public record Create(
            @NotNull(message = "Thiếu danh mục tài chính") Long financialCategoryId,
            @NotBlank(message = "Nội dung giao dịch không được để trống") String content,
            @NotNull(message = "Số tiền không được để trống")
            @DecimalMin(value = "0.001", message = "Số tiền phải lớn hơn 0") BigDecimal amount,
            @NotNull(message = "Thiếu loại giao dịch") TransactionType transactionType,
            UUID clientRequestId,
            LocalDateTime occurredAt,
            Long expectedPersonId) {
    }
}
