package ute.fit.financemanagement.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ute.fit.financemanagement.enums.TransactionStatus;
import ute.fit.financemanagement.enums.TransactionType;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Column(nullable = false)
    private Long financialCategoryId;

    @Column(columnDefinition = "text")
    private String content;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus transactionStatus = TransactionStatus.DRAFT;

    @Column(nullable = false)
    private Long createdPersonId;

    @Column(columnDefinition = "text")
    private String rawData;

    /** Thời điểm tạo — Mobile sắp xếp/nhóm danh sách giao dịch theo ngày. */
    @Builder.Default
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    public void approveTransaction() {
        requireAwaitingDecision();
        this.transactionStatus = TransactionStatus.APPROVED;
    }

    public void rejectTransaction() {
        requireAwaitingDecision();
        this.transactionStatus = TransactionStatus.REJECTED;
    }

    public void changeTransactionType(TransactionType newType) {
        if (transactionStatus == TransactionStatus.APPROVED
                || transactionStatus == TransactionStatus.REJECTED) {
            throw new IllegalStateException("Không đổi loại giao dịch đã được xử lý");
        }
        this.transactionType = newType;
    }

    private void requireAwaitingDecision() {
        if (transactionStatus != TransactionStatus.PENDING
                && transactionStatus != TransactionStatus.REVIEW) {
            throw new IllegalStateException(
                    "Chỉ duyệt/từ chối giao dịch đang PENDING hoặc REVIEW, hiện tại: " + transactionStatus);
        }
    }
}
