package ute.fit.financemanagement.service.impl;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ute.fit.financemanagement.dto.response.NotificationResponse;
import ute.fit.financemanagement.entity.Family;
import ute.fit.financemanagement.entity.FinancialCategory;
import ute.fit.financemanagement.entity.Person;
import ute.fit.financemanagement.entity.Transaction;
import ute.fit.financemanagement.enums.TransactionStatus;
import ute.fit.financemanagement.enums.TransactionType;
import ute.fit.financemanagement.repository.FamilyRepository;
import ute.fit.financemanagement.repository.FinancialCategoryRepository;
import ute.fit.financemanagement.repository.PersonRepository;
import ute.fit.financemanagement.repository.TransactionRepository;
import ute.fit.financemanagement.service.FamilyAccess;
import ute.fit.financemanagement.service.NotificationService;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    /** Cộng vào categoryId để id cảnh báo hạn mức không trùng id giao dịch cần duyệt. */
    private static final long WARNING_ID_OFFSET = 1_000_000_000L;

    private final FamilyRepository familyRepository;
    private final FinancialCategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final PersonRepository personRepository;
    private final FamilyAccess access;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> listMine(Long personId) {
        List<NotificationResponse> result = new ArrayList<>();
        for (Family family : familyRepository.findAllByMember(personId)) {
            boolean canApprove = access.canApprove(access.requireMember(family, personId));
            List<FinancialCategory> categories = categoryRepository
                    .findByFamilyIdOrderByFinancialCategoryId(family.getFamilyId());
            if (categories.isEmpty()) {
                continue;
            }
            List<Long> ids = categories.stream().map(FinancialCategory::getFinancialCategoryId).toList();
            Map<Long, FinancialCategory> byId = categories.stream()
                    .collect(Collectors.toMap(FinancialCategory::getFinancialCategoryId, Function.identity()));

            if (canApprove) {
                List<Transaction> pending = transactionRepository
                        .findByFinancialCategoryIdInAndTransactionStatusIn(ids,
                                List.of(TransactionStatus.PENDING, TransactionStatus.REVIEW));
                Map<Long, String> names = personRepository
                        .findAllById(pending.stream().map(Transaction::getCreatedPersonId).distinct().toList())
                        .stream().collect(Collectors.toMap(Person::getPersonId, Person::getName));
                for (Transaction t : pending) {
                    result.add(new NotificationResponse(t.getTransactionId(), "APPROVAL_REQUEST",
                            "Giao dịch cần duyệt",
                            lastWord(names.getOrDefault(t.getCreatedPersonId(), "Thành viên"))
                                    + " vừa tạo giao dịch \"" + t.getContent() + "\" "
                                    + money(t.getAmount()) + ", vượt hạn mức cần chấp thuận.",
                            ago(t.getCreatedAt()), true));
                }
            }
            for (FinancialCategory c : categories) {
                BigDecimal spent = transactionRepository.sumAmount(List.of(c.getFinancialCategoryId()),
                        TransactionType.EXPENSE, TransactionStatus.APPROVED);
                if (c.getInitialBalance().signum() > 0 && spent.signum() > 0
                        && spent.multiply(BigDecimal.valueOf(100))
                        .compareTo(c.getInitialBalance().multiply(BigDecimal.valueOf(80))) >= 0) {
                    double percent = spent.doubleValue() * 100 / c.getInitialBalance().doubleValue();
                    result.add(new NotificationResponse(WARNING_ID_OFFSET + c.getFinancialCategoryId(),
                            "THRESHOLD_WARNING", "Cảnh báo hạn mức hũ",
                            "Hũ " + c.getName() + " đã đạt " + String.format(Locale.US, "%.1f", percent)
                                    + "% định mức. Hãy cân nhắc các khoản chi sắp tới.",
                            "Hiện tại", false));
                }
            }
        }
        return result;
    }

    private static String ago(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        long minutes = Duration.between(time, LocalDateTime.now()).toMinutes();
        if (minutes < 1) {
            return "Vừa xong";
        }
        if (minutes < 60) {
            return minutes + " phút trước";
        }
        if (minutes < 24 * 60) {
            return minutes / 60 + " giờ trước";
        }
        return minutes / (24 * 60) + " ngày trước";
    }

    private static String money(BigDecimal amount) {
        DecimalFormat df = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.GERMANY));
        return df.format(amount) + " đ";
    }

    private static String lastWord(String name) {
        String[] parts = name.trim().split("\\s+");
        return parts[parts.length - 1];
    }
}