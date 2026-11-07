package ute.fit.financemanagement.config;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import ute.fit.financemanagement.entity.Account;
import ute.fit.financemanagement.entity.Family;
import ute.fit.financemanagement.entity.FinancialCategory;
import ute.fit.financemanagement.entity.Person;
import ute.fit.financemanagement.entity.Transaction;
import ute.fit.financemanagement.enums.AccountRole;
import ute.fit.financemanagement.enums.FamilyPermission;
import ute.fit.financemanagement.enums.TransactionStatus;
import ute.fit.financemanagement.enums.TransactionType;
import ute.fit.financemanagement.repository.AccountRepository;
import ute.fit.financemanagement.repository.FamilyRepository;
import ute.fit.financemanagement.repository.FinancialCategoryRepository;
import ute.fit.financemanagement.repository.PersonRepository;
import ute.fit.financemanagement.repository.TransactionRepository;

/**
 * Tạo tài khoản admin + gia đình mẫu (khớp dữ liệu demo của Mobile) khi DB còn chưa có admin.
 * Chỉ chạy khi SEED_ADMIN_PASSWORD có giá trị — mật khẩu không nằm trong mã nguồn.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final PersonRepository personRepository;
    private final AccountRepository accountRepository;
    private final FamilyRepository familyRepository;
    private final FinancialCategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate tx;

    @Value("${app.seed.admin-username:admin}")
    private String adminUsername;

    @Value("${app.seed.admin-password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.info("[Seed] Bỏ qua: chưa đặt SEED_ADMIN_PASSWORD");
            return;
        }
        if (accountRepository.findByUsernameIgnoreCase(adminUsername).isPresent()) {
            log.info("[Seed] Đã có tài khoản '{}', không seed lại", adminUsername);
            return;
        }
        tx.executeWithoutResult(status -> seed());
        log.info("[Seed] Đã tạo tài khoản '{}' và gia đình mẫu", adminUsername);
    }

    private void seed() {
        Person minh = person("Nguyễn Văn Minh", "minh.nguyen@email.com", "0912345678");
        Person lan = person("Trần Thị Thu Lan", "lan.tran@email.com", "0912000002");
        Person ngoc = person("Nguyễn Bảo Ngọc", "ngoc.nguyen@email.com", null);
        Person nguyen = person("Nguyễn Khôi Nguyên", "nguyen.nguyen@email.com", null);

        accountRepository.save(Account.builder()
                .personId(minh.getPersonId())
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .accountRole(AccountRole.ADMIN)
                .build());

        Family family = Family.create("Gia đình Minh & Lan", minh.getPersonId());
        List<FamilyPermission> rw = List.of(FamilyPermission.READ, FamilyPermission.WRITE);
        family.addMember(lan.getPersonId(), rw);
        family.addMember(ngoc.getPersonId(), rw);
        family.addMember(nguyen.getPersonId(), rw);
        family = familyRepository.save(family);

        // {tên, định mức, đã chi}
        Object[][] jars = {
                {"Nhu cầu thiết yếu", 22_000_000L, 14_200_000L},
                {"Giáo dục & con cái", 8_000_000L, 5_800_000L},
                {"Tiết kiệm dài hạn", 40_000_000L, 35_000_000L},
                {"Hưởng thụ & giải trí", 4_000_000L, 3_100_000L},
                {"Quỹ khẩn cấp & y tế", 15_000_000L, 15_000_000L},
                {"Cho đi & biếu tặng", 2_000_000L, 1_500_000L},
        };
        FinancialCategory first = null;
        for (Object[] jar : jars) {
            long limit = (Long) jar[1];
            FinancialCategory c = categoryRepository.save(family.createFinancialCategory(
                    (String) jar[0], null, money(limit), money(limit * 8 / 10), money(limit),
                    money(2_000_000)));
            first = first == null ? c : first;
            expense(c, minh, "Chi tiêu " + ((String) jar[0]).toLowerCase(), (Long) jar[2],
                    TransactionStatus.APPROVED);
        }
        income(first, minh, "Lương tháng của Minh", 48_500_000L);
        expense(first, nguyen, "Đồ chơi Lego", 2_450_000L, TransactionStatus.PENDING);
    }

    private Person person(String name, String email, String phone) {
        return personRepository.save(Person.builder().name(name).email(email).phone(phone).build());
    }

    private void expense(FinancialCategory c, Person by, String content, long amount, TransactionStatus s) {
        save(c, by, content, amount, TransactionType.EXPENSE, s);
    }

    private void income(FinancialCategory c, Person by, String content, long amount) {
        save(c, by, content, amount, TransactionType.INCOME, TransactionStatus.APPROVED);
    }

    private void save(FinancialCategory c, Person by, String content, long amount, TransactionType type,
                      TransactionStatus status) {
        transactionRepository.save(Transaction.builder()
                .financialCategoryId(c.getFinancialCategoryId())
                .content(content)
                .amount(money(amount))
                .transactionType(type)
                .transactionStatus(status)
                .createdPersonId(by.getPersonId())
                .build());
    }

    private static BigDecimal money(long value) {
        return BigDecimal.valueOf(value).setScale(3);
    }
}