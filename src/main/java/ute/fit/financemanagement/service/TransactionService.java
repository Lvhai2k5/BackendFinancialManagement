package ute.fit.financemanagement.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ute.fit.financemanagement.dto.request.TransactionRequests;
import ute.fit.financemanagement.dto.response.TransactionResponse;

public interface TransactionService {

    /** `familyId` null = giao dịch của mọi gia đình người dùng đang tham gia. */
    Page<TransactionResponse> list(Long personId, Long familyId, Pageable pageable);

    /** Chi tiết 1 giao dịch — mọi thành viên của gia đình đều được xem. */
    TransactionResponse get(Long transactionId, Long personId);

    TransactionResponse create(TransactionRequests.Create request, Long personId);

    TransactionResponse approve(Long transactionId, Long personId);

    TransactionResponse reject(Long transactionId, Long personId);
}