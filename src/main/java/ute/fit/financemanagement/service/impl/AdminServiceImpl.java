package ute.fit.financemanagement.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.AccountFamilyInfo;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.AccountStats;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.AccountSummary;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.ActivityLogItem;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.ActivityLogStats;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.AdminTicket;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.ChartPoint;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.Dashboard;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.PageResponse;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.TicketCounts;
import ute.fit.financemanagement.entity.Account;
import ute.fit.financemanagement.entity.ActivityLog;
import ute.fit.financemanagement.entity.Family;
import ute.fit.financemanagement.entity.Person;
import ute.fit.financemanagement.entity.SupportTicket;
import ute.fit.financemanagement.enums.AccountRole;
import ute.fit.financemanagement.enums.AccountStatus;
import ute.fit.financemanagement.enums.ActionType;
import ute.fit.financemanagement.enums.ActivityStatus;
import ute.fit.financemanagement.enums.TicketStatus;
import ute.fit.financemanagement.exception.ApiException;
import ute.fit.financemanagement.repository.AccountRepository;
import ute.fit.financemanagement.repository.ActivityLogRepository;
import ute.fit.financemanagement.repository.AdminQueryRepository;
import ute.fit.financemanagement.repository.AdminQueryRepository.AccountRow;
import ute.fit.financemanagement.repository.AdminQueryRepository.LogRow;
import ute.fit.financemanagement.repository.AdminQueryRepository.LogTimes;
import ute.fit.financemanagement.repository.AdminQueryRepository.TicketRow;
import ute.fit.financemanagement.repository.FamilyRepository;
import ute.fit.financemanagement.repository.PersonRepository;
import ute.fit.financemanagement.repository.SupportTicketRepository;
import ute.fit.financemanagement.security.CurrentUser;
import ute.fit.financemanagement.service.AdminService;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminServiceImpl implements AdminService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_CHART_DAYS = 90;

    private final AdminQueryRepository query;
    private final AccountRepository accountRepository;
    private final PersonRepository personRepository;
    private final FamilyRepository familyRepository;
    private final SupportTicketRepository ticketRepository;
    private final ActivityLogRepository activityLogRepository;

    // ---------- Dashboard ----------

    @Override
    @Transactional(readOnly = true)
    public Dashboard dashboard(int days) {
        requireAdmin();
        int range = Math.min(Math.max(days, 1), MAX_CHART_DAYS);
        LocalDate today = LocalDate.now();
        LocalDate startDay = today.minusDays(range - 1L);

        TreeMap<LocalDate, long[]> buckets = new TreeMap<>();
        for (LocalDate d = startDay; !d.isAfter(today); d = d.plusDays(1)) {
            buckets.put(d, new long[2]);
        }
        for (LogTimes t : query.logTimesForChart(startDay.atStartOfDay())) {
            long[] b = buckets.get(t.time().toLocalDate());
            if (b == null) {
                continue;
            }
            if (t.actionType() == ActionType.REGISTER) {
                b[0]++;
            } else {
                b[1]++;
            }
        }
        List<ChartPoint> chart = new ArrayList<>();
        buckets.forEach((d, b) -> chart.add(new ChartPoint(d.toString(), b[0], b[1])));

        List<ActivityLogItem> recent = query
                .searchLogs(null, null, null, null, null, PageRequest.of(0, 5)).getContent()
                .stream().map(AdminServiceImpl::toLogItem).toList();

        return new Dashboard(accountStats(), ticketCounts(), countWarningCategories(),
                query.countLogsSince(ActionType.LOGIN, ActivityStatus.FAILED, LocalDateTime.now().minusHours(24)),
                chart, recent);
    }

    /** Số danh mục có số tiền đã sử dụng (Expense đã APPROVED) >= amountOfWarning. */
    private long countWarningCategories() {
        Map<Long, BigDecimal> used = query.approvedExpenseByCategory();
        return query.categoryWarnings().stream()
                .filter(c -> used.getOrDefault(c.categoryId(), BigDecimal.ZERO).compareTo(c.warning()) >= 0)
                .count();
    }

    // ---------- Accounts ----------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccountSummary> listAccounts(String keyword, AccountRole role, AccountStatus status,
                                                     int page, int size) {
        requireAdmin();
        var result = query.searchAccounts(keyword, role, status, pageable(page, size));
        return PageResponse.of(result.map(this::toAccountSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountStats accountStats() {
        requireAdmin();
        return new AccountStats(query.countAccounts(null, null),
                query.countAccounts(AccountStatus.ACTIVE, null),
                query.countAccounts(AccountStatus.INACTIVE, null),
                query.countAccounts(null, AccountRole.ADMIN));
    }

    @Override
    public AccountSummary lockAccount(Long accountId, String ip) {
        return changeAccountStatus(accountId, ip, true);
    }

    @Override
    public AccountSummary unlockAccount(Long accountId, String ip) {
        return changeAccountStatus(accountId, ip, false);
    }

    private AccountSummary changeAccountStatus(Long accountId, String ip, boolean lock) {
        CurrentUser admin = requireAdmin();
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản #" + accountId));
        if (account.getAccountRole() == AccountRole.ADMIN) {
            throw ApiException.forbidden("Không thể khóa/mở khóa tài khoản quản trị viên");
        }
        if (lock && account.getAccountStatus() == AccountStatus.INACTIVE) {
            throw ApiException.conflict("Tài khoản đã bị khóa");
        }
        if (!lock && account.getAccountStatus() == AccountStatus.ACTIVE) {
            throw ApiException.conflict("Tài khoản đang hoạt động");
        }
        if (lock) {
            account.lockAccount();
        } else {
            account.unlockAccount();
        }
        accountRepository.save(account);
        log(ActionType.MODIFY, ActivityStatus.SUCCESS,
                (lock ? "Admin lock account #" : "Admin unlock account #") + accountId, ip,
                admin.personId());
        Person person = personRepository.findById(account.getPersonId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ người dùng"));
        return toAccountSummary(new AccountRow(account, person));
    }

    // ---------- Support tickets ----------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminTicket> listTickets(String keyword, TicketStatus status, int page, int size) {
        requireAdmin();
        if (status == TicketStatus.DRAFT) {
            throw ApiException.badRequest("Admin không xem phiếu nháp");
        }
        return PageResponse.of(query.searchTickets(keyword, status, pageable(page, size))
                .map(AdminServiceImpl::toAdminTicket));
    }

    @Override
    @Transactional(readOnly = true)
    public TicketCounts ticketCounts() {
        requireAdmin();
        long submitted = query.countTickets(TicketStatus.SUBMITTED);
        long viewed = query.countTickets(TicketStatus.VIEWED);
        long responded = query.countTickets(TicketStatus.RESPONDED);
        return new TicketCounts(submitted + viewed + responded, submitted, viewed, responded);
    }

    @Override
    public AdminTicket markTicketViewed(Long ticketId, String ip) {
        CurrentUser admin = requireAdmin();
        SupportTicket ticket = findSubmittedTicket(ticketId);
        if (ticket.getTicketStatus() == TicketStatus.SUBMITTED) {
            ticket.changeTicketStatus(TicketStatus.VIEWED);
            ticketRepository.save(ticket);
            log(ActionType.MODIFY, ActivityStatus.SUCCESS, "Admin view ticket #" + ticketId, ip,
                    admin.personId());
        }
        return toAdminTicket(query.findTicket(ticketId));
    }

    @Override
    public AdminTicket respondTicket(Long ticketId, String noteOfRespondent, String ip) {
        CurrentUser admin = requireAdmin();
        SupportTicket ticket = findSubmittedTicket(ticketId);
        if (ticket.getTicketStatus() == TicketStatus.RESPONDED) {
            throw ApiException.conflict("Phiếu hỗ trợ này đã được phản hồi");
        }
        if (ticket.getTicketStatus() == TicketStatus.SUBMITTED) {
            ticket.changeTicketStatus(TicketStatus.VIEWED); // luồng bắt buộc: Submitted → Viewed → Responded
        }
        ticket.handleWithTicket(admin.personId(), noteOfRespondent.trim());
        ticketRepository.save(ticket);
        log(ActionType.MODIFY, ActivityStatus.SUCCESS, "Admin respond ticket #" + ticketId, ip,
                admin.personId());
        return toAdminTicket(query.findTicket(ticketId));
    }

    private SupportTicket findSubmittedTicket(Long ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phiếu hỗ trợ #" + ticketId));
        if (ticket.getTicketStatus() == TicketStatus.DRAFT) {
            throw ApiException.notFound("Không tìm thấy phiếu hỗ trợ #" + ticketId);
        }
        return ticket;
    }

    // ---------- Activity logs ----------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ActivityLogItem> listLogs(String keyword, ActionType action, ActivityStatus status,
                                                  LocalDate from, LocalDate to, int page, int size) {
        requireAdmin();
        if (from != null && to != null && to.isBefore(from)) {
            throw ApiException.badRequest("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }
        var result = query.searchLogs(keyword, action, status,
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay(), pageable(page, size));
        return PageResponse.of(result.map(AdminServiceImpl::toLogItem));
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityLogStats logStats() {
        requireAdmin();
        return new ActivityLogStats(query.countLogs(null), query.countLogs(ActivityStatus.SUCCESS),
                query.countLogs(ActivityStatus.FAILED));
    }

    // ---------- helpers ----------

    private static CurrentUser requireAdmin() {
        CurrentUser user = CurrentUser.get();
        if (user.role() != AccountRole.ADMIN) {
            throw ApiException.forbidden("Chỉ quản trị viên được truy cập chức năng này");
        }
        return user;
    }

    private static Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    private void log(ActionType type, ActivityStatus status, String description, String ip, Long personId) {
        activityLogRepository.save(ActivityLog.builder()
                .actionType(type)
                .activityStatus(status)
                .activityDescription(description)
                .ipAddress(ip)
                .performedPersonId(personId)
                .build());
    }

    private AccountSummary toAccountSummary(AccountRow row) {
        Account a = row.account();
        Person p = row.person();
        List<AccountFamilyInfo> families = new ArrayList<>();
        for (Family f : familyRepository.findAllByMember(p.getPersonId())) {
            String role = f.getMemberships().stream()
                    .filter(m -> m.getPersonId().equals(p.getPersonId()))
                    .map(m -> m.getFamilyMemberRole().name()).findFirst().orElse("MEMBER");
            families.add(new AccountFamilyInfo(f.getFamilyId(), f.getName(), f.getMemberships().size(), role));
        }
        return new AccountSummary(a.getAccountId(), p.getPersonId(), a.getUsername(), p.getName(), p.getEmail(),
                p.getPhone(), p.getAvatar(), a.getAccountRole().name(), a.getAccountStatus().name(), families);
    }

    private static AdminTicket toAdminTicket(TicketRow row) {
        SupportTicket t = row.ticket();
        return new AdminTicket(t.getSupportTicketId(), t.getContent(), t.getNoteOfRespondent(),
                t.getCreatedPersonId(), t.getAnsweredPersonId(), t.getTicketStatus().name(),
                row.creator().getName(), row.creator().getEmail(), t.getCreatedAt());
    }

    private static ActivityLogItem toLogItem(LogRow row) {
        ActivityLog l = row.log();
        Person p = row.person();
        return new ActivityLogItem(l.getLogId(), l.getActionType().name(), l.getActivityStatus().name(),
                l.getActivityDescription(), l.getIpAddress(), l.getLogTime(), l.getPerformedPersonId(),
                p == null ? null : p.getName(), p == null ? null : p.getEmail());
    }
}
