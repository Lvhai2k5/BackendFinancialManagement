package ute.fit.financemanagement.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ute.fit.financemanagement.dto.request.FamilyRequests;
import ute.fit.financemanagement.dto.response.FamilyResponses;
import ute.fit.financemanagement.entity.Account;
import ute.fit.financemanagement.entity.Family;
import ute.fit.financemanagement.enums.FamilyMemberRole;
import ute.fit.financemanagement.repository.AccountRepository;
import ute.fit.financemanagement.entity.FinancialCategory;
import ute.fit.financemanagement.entity.Membership;
import ute.fit.financemanagement.entity.Person;
import ute.fit.financemanagement.enums.CategoryStatus;
import ute.fit.financemanagement.enums.FamilyPermission;
import ute.fit.financemanagement.enums.TransactionStatus;
import ute.fit.financemanagement.enums.TransactionType;
import ute.fit.financemanagement.exception.ApiException;
import ute.fit.financemanagement.repository.FamilyRepository;
import ute.fit.financemanagement.repository.FinancialCategoryRepository;
import ute.fit.financemanagement.repository.PersonRepository;
import ute.fit.financemanagement.repository.TransactionRepository;
import ute.fit.financemanagement.service.FamilyAccess;
import ute.fit.financemanagement.service.FamilyService;

@Service
@RequiredArgsConstructor
public class FamilyServiceImpl implements FamilyService {

    private static final String CODE_PREFIX = "#FN-";

    private final FamilyRepository familyRepository;
    private final FinancialCategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final PersonRepository personRepository;
    private final AccountRepository accountRepository;
    private final FamilyAccess access;

    @Override
    @Transactional(readOnly = true)
    public List<FamilyResponses.Summary> listMine(Long personId) {
        return familyRepository.findAllByMember(personId).stream()
                .map(f -> summary(f, personId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FamilyResponses.Detail detail(Long familyId, Long personId) {
        Family family = access.requireFamily(familyId);
        access.requireMember(family, personId);
        return detailOf(family, personId);
    }

    @Override
    @Transactional
    public FamilyResponses.Detail create(FamilyRequests.CreateFamily request, Long personId) {
        Family family = familyRepository.save(Family.create(request.name().trim(), personId));
        return detailOf(family, personId);
    }

    @Override
    @Transactional
    public FamilyResponses.Detail join(FamilyRequests.JoinFamily request, Long personId) {
        Family family = access.requireFamily(parseCode(request.code()));
        if (family.getMemberships().stream().anyMatch(m -> m.getPersonId().equals(personId))) {
            throw ApiException.conflict("Bạn đã là thành viên của gia đình này");
        }
        family.addMember(personId, List.of(FamilyPermission.READ, FamilyPermission.WRITE));
        familyRepository.save(family);
        return detailOf(family, personId);
    }

    @Override
    @Transactional
    public FamilyResponses.Detail addMember(Long familyId, FamilyRequests.AddMember request, Long personId) {
        Family family = access.requireFamily(familyId);
        access.requireOwner(family, personId);
        Long memberId = resolveMemberId(request);
        if (family.getMemberships().stream().anyMatch(m -> m.getPersonId().equals(memberId))) {
            throw ApiException.conflict("Người này đã là thành viên của gia đình");
        }
        family.addMember(memberId, List.of(FamilyPermission.READ, FamilyPermission.WRITE));
        familyRepository.save(family);
        return detailOf(family, personId);
    }

    /** Tìm người cần thêm: ưu tiên email, không có thì dùng tên đăng nhập. */
    private Long resolveMemberId(FamilyRequests.AddMember request) {
        String email = request.email() == null ? "" : request.email().trim();
        String username = request.username() == null ? "" : request.username().trim();
        if (!email.isEmpty()) {
            return personRepository.findByEmailIgnoreCase(email)
                    .map(Person::getPersonId)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản FinNest với email này"));
        }
        if (!username.isEmpty()) {
            return accountRepository.findByUsernameIgnoreCase(username)
                    .map(Account::getPersonId)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản FinNest này"));
        }
        throw ApiException.badRequest("Vui lòng nhập email của người cần thêm");
    }

    @Override
    @Transactional
    public FamilyResponses.Detail removeMember(Long familyId, Long memberId, Long personId) {
        Family family = access.requireFamily(familyId);
        access.requireOwner(family, personId);
        Membership target = requireTarget(family, memberId);
        if (access.isOwner(target)) {
            throw ApiException.badRequest("Không thể xóa Chủ hộ. Hãy chuyển vai trò Chủ hộ trước.");
        }
        family.removeMember(memberId);
        familyRepository.save(family);
        return detailOf(family, personId);
    }

    @Override
    @Transactional
    public FamilyResponses.Detail updatePermissions(Long familyId, Long memberId,
                                                    FamilyRequests.UpdatePermissions request, Long personId) {
        Family family = access.requireFamily(familyId);
        access.requireOwner(family, personId);
        if (access.isOwner(requireTarget(family, memberId))) {
            throw ApiException.badRequest("Chủ hộ luôn có đầy đủ quyền.");
        }
        family.adjustPermission(memberId, request.permissions().stream().distinct().toList());
        familyRepository.save(family);
        return detailOf(family, personId);
    }

    @Override
    @Transactional
    public FamilyResponses.Detail transferOwnership(Long familyId, Long memberId, Long personId) {
        Family family = access.requireFamily(familyId);
        access.requireOwner(family, personId);
        if (access.isOwner(requireTarget(family, memberId))) {
            throw ApiException.badRequest("Người này đã là Chủ hộ.");
        }
        family.changeFamilyMemberRole(memberId, FamilyMemberRole.OWNER);
        family.adjustPermission(memberId, List.of(FamilyPermission.values()));
        family.changeFamilyMemberRole(personId, FamilyMemberRole.MEMBER);
        family.adjustPermission(personId, List.of(FamilyPermission.READ, FamilyPermission.WRITE));
        familyRepository.save(family);
        return detailOf(family, personId);
    }

    private Membership requireTarget(Family family, Long memberId) {
        return family.getMemberships().stream()
                .filter(m -> m.getPersonId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Thành viên không thuộc gia đình này"));
    }

    @Override
    @Transactional
    public FamilyResponses.Category createCategory(Long familyId, FamilyRequests.CreateCategory request,
                                                   Long personId) {
        Family family = access.requireFamily(familyId);
        access.requireOwner(family, personId);
        FinancialCategory category = categoryRepository.save(family.createFinancialCategory(
                request.name().trim(), request.description(),
                money(request.initialBalance()), money(request.amountOfWarning()),
                money(request.amountOfEmergency()), money(request.amountOfApproval())));
        return categoryResponse(category, BigDecimal.ZERO, 100);
    }

    @Override
    @Transactional
    public FamilyResponses.Category updateThreshold(Long categoryId, FamilyRequests.UpdateThreshold request,
                                                    Long personId) {
        FinancialCategory category = requireCategoryAsOwner(categoryId, personId);
        category.modifyThreshold(money(request.amountOfWarning()), money(request.amountOfEmergency()),
                money(request.amountOfApproval()));
        categoryRepository.save(category);
        return categoryResponse(category, spent(category.getFinancialCategoryId()), 0);
    }

    @Override
    @Transactional
    public FamilyResponses.Category setLocked(Long categoryId, boolean locked, Long personId) {
        FinancialCategory category = requireCategoryAsOwner(categoryId, personId);
        if (locked) {
            category.lockCategory();
        } else {
            category.unlockCategory();
        }
        categoryRepository.save(category);
        return categoryResponse(category, spent(category.getFinancialCategoryId()), 0);
    }

    // ---------- helpers ----------

    private FinancialCategory requireCategoryAsOwner(Long categoryId, Long personId) {
        FinancialCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy danh mục tài chính"));
        access.requireOwner(access.requireFamily(category.getFamilyId()), personId);
        return category;
    }

    private FamilyResponses.Summary summary(Family family, Long personId) {
        List<FinancialCategory> categories = categoryRepository
                .findByFamilyIdOrderByFinancialCategoryId(family.getFamilyId());
        return new FamilyResponses.Summary(family.getFamilyId(), code(family), family.getName(),
                roleOf(family, personId), totalBudget(categories), categories.size(), members(family));
    }

    private FamilyResponses.Detail detailOf(Family family, Long personId) {
        List<FinancialCategory> categories = categoryRepository
                .findByFamilyIdOrderByFinancialCategoryId(family.getFamilyId());
        List<Long> ids = ids(categories);
        BigDecimal income = ids.isEmpty() ? BigDecimal.ZERO
                : transactionRepository.sumAmount(ids, TransactionType.INCOME, TransactionStatus.APPROVED);
        BigDecimal expense = ids.isEmpty() ? BigDecimal.ZERO
                : transactionRepository.sumAmount(ids, TransactionType.EXPENSE, TransactionStatus.APPROVED);

        BigDecimal totalLimit = categories.stream().map(FinancialCategory::getInitialBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<FamilyResponses.Category> categoryResponses = new ArrayList<>();
        for (FinancialCategory c : categories) {
            int percent = totalLimit.signum() == 0 ? 0
                    : c.getInitialBalance().multiply(BigDecimal.valueOf(100))
                    .divide(totalLimit, 0, RoundingMode.HALF_UP).intValue();
            categoryResponses.add(categoryResponse(c, spent(c.getFinancialCategoryId()), percent));
        }
        return new FamilyResponses.Detail(family.getFamilyId(), code(family), family.getName(),
                roleOf(family, personId), totalLimit.add(income).subtract(expense), 0.0, "",
                members(family), categoryResponses, income, expense,
                access.requireMember(family, personId).getPermissions().stream().map(Enum::name).toList());
    }

    private FamilyResponses.Category categoryResponse(FinancialCategory c, BigDecimal spent, int percent) {
        return new FamilyResponses.Category(c.getFinancialCategoryId(), c.getName(), percent, spent,
                c.getInitialBalance(), c.getCategoryStatus().name(), c.getAmountOfWarning(),
                c.getAmountOfEmergency(), c.getAmountOfApproval());
    }

    private BigDecimal spent(Long categoryId) {
        return transactionRepository.sumAmount(List.of(categoryId), TransactionType.EXPENSE,
                TransactionStatus.APPROVED);
    }

    private BigDecimal totalBudget(List<FinancialCategory> categories) {
        List<Long> ids = ids(categories);
        BigDecimal limit = categories.stream().map(FinancialCategory::getInitialBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (ids.isEmpty()) {
            return limit;
        }
        return limit
                .add(transactionRepository.sumAmount(ids, TransactionType.INCOME, TransactionStatus.APPROVED))
                .subtract(transactionRepository.sumAmount(ids, TransactionType.EXPENSE, TransactionStatus.APPROVED));
    }

    private List<FamilyResponses.Member> members(Family family) {
        Map<Long, Person> persons = personRepository
                .findAllById(family.getMemberships().stream().map(Membership::getPersonId).toList())
                .stream().collect(Collectors.toMap(Person::getPersonId, Function.identity()));
        return family.getMemberships().stream()
                .map(m -> new FamilyResponses.Member(m.getPersonId(),
                        persons.containsKey(m.getPersonId()) ? persons.get(m.getPersonId()).getName() : "?",
                        m.getFamilyMemberRole().name(),
                        m.getPermissions().stream().map(Enum::name).toList()))
                .toList();
    }

    private String roleOf(Family family, Long personId) {
        return access.requireMember(family, personId).getFamilyMemberRole().name();
    }

    private static List<Long> ids(Collection<FinancialCategory> categories) {
        return categories.stream().map(FinancialCategory::getFinancialCategoryId).toList();
    }

    static String code(Family family) {
        return CODE_PREFIX + family.getFamilyId();
    }

    private static Long parseCode(String code) {
        String digits = code.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            throw ApiException.badRequest("Mã gia đình không hợp lệ");
        }
        return Long.valueOf(digits);
    }

    /** Làm tròn 3 chữ số thập phân ngay khi vào entity (TECH-ORM.md mục 3). */
    private static BigDecimal money(BigDecimal value) {
        return value.setScale(3, RoundingMode.HALF_UP);
    }
}