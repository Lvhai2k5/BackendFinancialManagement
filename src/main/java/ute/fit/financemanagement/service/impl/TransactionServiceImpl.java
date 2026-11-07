package ute.fit.financemanagement.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ute.fit.financemanagement.dto.request.TransactionRequests;
import ute.fit.financemanagement.dto.response.TransactionResponse;
import ute.fit.financemanagement.entity.Family;
import ute.fit.financemanagement.entity.FinancialCategory;
import ute.fit.financemanagement.entity.Membership;
import ute.fit.financemanagement.entity.Person;
import ute.fit.financemanagement.entity.Transaction;
import ute.fit.financemanagement.enums.CategoryStatus;
import ute.fit.financemanagement.enums.TransactionStatus;
import ute.fit.financemanagement.enums.TransactionType;
import ute.fit.financemanagement.exception.ApiException;
import ute.fit.financemanagement.repository.FamilyRepository;
import ute.fit.financemanagement.repository.FinancialCategoryRepository;
import ute.fit.financemanagement.repository.PersonRepository;
import ute.fit.financemanagement.repository.TransactionRepository;
import ute.fit.financemanagement.service.FamilyAccess;
import ute.fit.financemanagement.service.TransactionService;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final FinancialCategoryRepository categoryRepository;
    private final FamilyRepository familyRepository;
    private final PersonRepository personRepository;
    private final FamilyAccess access;

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(Long personId, Long familyId, Pageable pageable) {
        List<Family> families;
        if (familyId != null) {
            Family family = access.requireFamily(familyId);
            access.requireMember(family, personId);
            families = List.of(family);
        } else {
            families = familyRepository.findAllByMember(personId);
        }
        List<FinancialCategory> categories = categoryRepository.findByFamilyIdIn(
                families.stream().map(Family::getFamilyId).toList());
        if (categories.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        Page<Transaction> page = transactionRepository.findByFinancialCategoryIdIn(
                categories.stream().map(FinancialCategory::getFinancialCategoryId).toList(), pageable);
        return toResponses(page, categories);
    }

    @Override
    @Transactional
    public TransactionResponse create(TransactionRequests.Create request, Long personId) {
        FinancialCategory category = requireCategory(request.financialCategoryId());
        Family family = access.requireFamily(category.getFamilyId());
        Membership membership = access.requireMember(family, personId);
        if (!access.canWrite(membership)) {
            throw ApiException.forbidden("Bạn không có quyền ghi giao dịch trong gia đình này");
        }
        if (category.getCategoryStatus() == CategoryStatus.LOCKED) {
            throw ApiException.conflict("Danh mục đang bị khoá, không thể ghi giao dịch");
        }

        BigDecimal amount = request.amount().setScale(3, RoundingMode.HALF_UP);
        // Chi vượt ngưỡng cần duyệt của hũ (Threshold Approve) -> chờ Chủ hộ duyệt.
        boolean needApproval = request.transactionType() == TransactionType.EXPENSE
                && category.getAmountOfApproval().signum() > 0
                && amount.compareTo(category.getAmountOfApproval()) >= 0
                && !access.isOwner(membership);

        Transaction saved = transactionRepository.save(Transaction.builder()
                .financialCategoryId(category.getFinancialCategoryId())
                .content(request.content().trim())
                .amount(amount)
                .transactionType(request.transactionType())
                .transactionStatus(needApproval ? TransactionStatus.PENDING : TransactionStatus.APPROVED)
                .createdPersonId(personId)
                .build());
        return toResponses(List.of(saved), List.of(category)).get(0);
    }

    @Override
    @Transactional
    public TransactionResponse approve(Long transactionId, Long personId) {
        return decide(transactionId, personId, true);
    }

    @Override
    @Transactional
    public TransactionResponse reject(Long transactionId, Long personId) {
        return decide(transactionId, personId, false);
    }

    private TransactionResponse decide(Long transactionId, Long personId, boolean approve) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch"));
        FinancialCategory category = requireCategory(tx.getFinancialCategoryId());
        Family family = access.requireFamily(category.getFamilyId());
        if (!access.canApprove(access.requireMember(family, personId))) {
            throw ApiException.forbidden("Bạn không có quyền duyệt giao dịch");
        }
        if (approve) {
            tx.approveTransaction();
        } else {
            tx.rejectTransaction();
        }
        transactionRepository.save(tx);
        return toResponses(List.of(tx), List.of(category)).get(0);
    }

    private FinancialCategory requireCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy danh mục tài chính"));
    }

    private Page<TransactionResponse> toResponses(Page<Transaction> page, List<FinancialCategory> categories) {
        List<TransactionResponse> content = toResponses(page.getContent(), categories);
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    private List<TransactionResponse> toResponses(List<Transaction> transactions,
                                                  List<FinancialCategory> categories) {
        Map<Long, FinancialCategory> byId = categories.stream()
                .collect(Collectors.toMap(FinancialCategory::getFinancialCategoryId, Function.identity()));
        Map<Long, String> names = personRepository
                .findAllById(transactions.stream().map(Transaction::getCreatedPersonId).distinct().toList())
                .stream().collect(Collectors.toMap(Person::getPersonId, Person::getName));
        return transactions.stream().map(t -> {
            FinancialCategory c = byId.get(t.getFinancialCategoryId());
            return new TransactionResponse(t.getTransactionId(), t.getContent(), t.getAmount(),
                    t.getTransactionType().name(), t.getTransactionStatus().name(),
                    t.getFinancialCategoryId(), c == null ? "" : c.getName(),
                    c == null ? null : c.getFamilyId(), t.getCreatedPersonId(),
                    names.getOrDefault(t.getCreatedPersonId(), "?"), t.getCreatedAt());
        }).toList();
    }
}