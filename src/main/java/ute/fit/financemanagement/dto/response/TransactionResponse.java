package ute.fit.financemanagement.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** type: INCOME | EXPENSE; status: DRAFT/PENDING/REVIEW/APPROVED/REJECTED/OFFLINE. */
public record TransactionResponse(Long transactionId, String content, BigDecimal amount,
                                  String type, String status, Long financialCategoryId,
                                  String categoryName, Long familyId, Long createdPersonId,
                                  String createdPersonName, LocalDateTime createdAt) {
}