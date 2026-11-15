package ute.fit.financemanagement.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import ute.fit.financemanagement.enums.ActionType;
import ute.fit.financemanagement.enums.TransactionStatus;
import ute.fit.financemanagement.enums.TransactionType;
import ute.fit.financemanagement.exception.ApiException;
import ute.fit.financemanagement.repository.FamilyRepository;
import ute.fit.financemanagement.repository.FinancialCategoryRepository;
import ute.fit.financemanagement.repository.PersonRepository;
import ute.fit.financemanagement.repository.TransactionRepository;
import ute.fit.financemanagement.service.FamilyAccess;
import ute.fit.financemanagement.service.ActivityAudit;
import ute.fit.financemanagement.service.TransactionService;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final FinancialCategoryRepository categoryRepository;
    private final FamilyRepository familyRepository;
    private final PersonRepository personRepository;
    private final FamilyAccess access;
    private final ActivityAudit activityAudit;

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(Long personId, Long familyId, Pageable pageable) {
        List<Family> families;
        if (familyId != null) {
            Family family = access.requireFamily(familyId);
            Membership membership = access.requireMember(family, personId);
            if (!access.canRead(membership)) {
                throw ApiException.forbidden("Bạn không có quyền xem giao dịch của gia đình này");
            }
            families = List.of(family);
        } else {
            families = familyRepository.findAllByMember(personId).stream()
                    .filter(family -> access.canRead(access.requireMember(family, personId)))
                    .toList();
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
    @Transactional(readOnly = true)
    public TransactionResponse get(Long transactionId, Long personId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch"));
        FinancialCategory category = requireCategory(tx.getFinancialCategoryId());
        Membership membership = access.requireMember(access.requireFamily(category.getFamilyId()), personId);
        if (!access.canRead(membership)) {
            throw ApiException.forbidden("Bạn không có quyền xem giao dịch này");
        }
        return toResponses(List.of(tx), List.of(category)).get(0);
    }

    @Override
    @Transactional
    public TransactionResponse create(TransactionRequests.Create request, Long personId) {
        if (request.expectedPersonId() != null && !request.expectedPersonId().equals(personId)) {
            throw ApiException.forbidden("Giao dịch ngoại tuyến thuộc về tài khoản khác");
        }

        Long familyId = categoryRepository.findFamilyId(request.financialCategoryId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy danh mục tài chính"));
        Family family = request.clientRequestId() == null
                ? access.requireFamily(familyId)
                : familyRepository.findForUpdate(familyId)
                        .orElseThrow(() -> ApiException.notFound("Không tìm thấy gia đình"));
        Membership membership = access.requireMember(family, personId);
        if (!access.canWrite(membership)) {
            throw ApiException.forbidden("Bạn không có quyền ghi giao dịch trong gia đình này");
        }

        if (request.clientRequestId() != null) {
            Transaction previous = transactionRepository
                    .findByCreatedPersonIdAndClientRequestId(personId, request.clientRequestId())
                    .orElse(null);
            if (previous != null) {
                if (!sameRequest(previous, request)) {
                    throw ApiException.conflict("Mã yêu cầu đã được dùng cho nội dung giao dịch khác");
                }
                FinancialCategory previousCategory = requireCategory(previous.getFinancialCategoryId());
                return toResponses(List.of(previous), List.of(previousCategory)).get(0);
            }
        }

        FinancialCategory category = requireCategory(request.financialCategoryId());
        if (category.getCategoryStatus() == CategoryStatus.LOCKED) {
            throw ApiException.conflict("Danh mục đang bị khoá, không thể ghi giao dịch");
        }

        BigDecimal amount = request.amount().setScale(3, RoundingMode.HALF_UP);
        // Chi vượt ngưỡng cần duyệt của hũ (Threshold Approve) -> chờ Chủ hộ duyệt.
        boolean needApproval = request.transactionType() == TransactionType.EXPENSE
                && category.getAmountOfApproval().signum() > 0
                && amount.compareTo(category.getAmountOfApproval()) >= 0
                && !access.isOwner(membership);

        Transaction.TransactionBuilder transaction = Transaction.builder()
                .financialCategoryId(category.getFinancialCategoryId())
                .content(request.content().trim())
                .amount(amount)
                .transactionType(request.transactionType())
                .transactionStatus(needApproval ? TransactionStatus.PENDING : TransactionStatus.APPROVED)
                .createdPersonId(personId)
                .clientRequestId(request.clientRequestId())
                // Nhập thủ công: dữ liệu gốc chính là nội dung người dùng đã nhập, giữ làm bằng chứng.
                .rawData(request.content());
        if (request.occurredAt() != null) {
            transaction.createdAt(request.occurredAt());
        }
        Transaction saved = transactionRepository.save(transaction.build());
        activityAudit.record(ActionType.CREATE, personId,
                "Tạo giao dịch " + saved.getTransactionId());
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
        activityAudit.record(ActionType.MODIFY, personId,
                (approve ? "Duyệt" : "Từ chối") + " giao dịch " + tx.getTransactionId());
        return toResponses(List.of(tx), List.of(category)).get(0);
    }

    private boolean sameRequest(Transaction previous, TransactionRequests.Create request) {
        return Objects.equals(previous.getFinancialCategoryId(), request.financialCategoryId())
                && Objects.equals(previous.getContent(), request.content().trim())
                && previous.getAmount().compareTo(request.amount().setScale(3, RoundingMode.HALF_UP)) == 0
                && previous.getTransactionType() == request.transactionType();
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
