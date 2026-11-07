package ute.fit.financemanagement.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ute.fit.financemanagement.entity.Account;
import ute.fit.financemanagement.entity.ActivityLog;
import ute.fit.financemanagement.entity.Person;
import ute.fit.financemanagement.entity.SupportTicket;
import ute.fit.financemanagement.enums.AccountRole;
import ute.fit.financemanagement.enums.AccountStatus;
import ute.fit.financemanagement.enums.ActionType;
import ute.fit.financemanagement.enums.ActivityStatus;
import ute.fit.financemanagement.enums.TicketStatus;

/**
 * Truy vấn JPQL cho khu vực Admin. Các entity chỉ tham chiếu nhau bằng id (DDD) nên join "theta" qua id.
 * Tách riêng khỏi các JpaRepository để không đụng vào repository dùng cho Mobile.
 */
@Repository
public class AdminQueryRepository {

    @PersistenceContext
    private EntityManager em;

    // ---------- Accounts ----------

    public record AccountRow(Account account, Person person) {
    }

    public Page<AccountRow> searchAccounts(String keyword, AccountRole role, AccountStatus status, Pageable pageable) {
        StringBuilder from = new StringBuilder(" from Account a, Person p where a.personId = p.personId");
        Map<String, Object> params = new HashMap<>();
        if (role != null) {
            from.append(" and a.accountRole = :role");
            params.put("role", role);
        }
        if (status != null) {
            from.append(" and a.accountStatus = :status");
            params.put("status", status);
        }
        if (hasText(keyword)) {
            from.append(" and (lower(a.username) like :kw or lower(p.name) like :kw"
                    + " or lower(p.email) like :kw or lower(coalesce(p.phone, '')) like :kw)");
            params.put("kw", like(keyword));
        }
        long total = count("select count(a)" + from, params);
        TypedQuery<Object[]> q = em.createQuery("select a, p" + from + " order by a.accountId desc", Object[].class);
        params.forEach(q::setParameter);
        q.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize());
        List<AccountRow> rows = q.getResultList().stream()
                .map(r -> new AccountRow((Account) r[0], (Person) r[1])).toList();
        return new PageImpl<>(rows, pageable, total);
    }

    public long countAccounts(AccountStatus status, AccountRole role) {
        StringBuilder jpql = new StringBuilder("select count(a) from Account a where 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (status != null) {
            jpql.append(" and a.accountStatus = :status");
            params.put("status", status);
        }
        if (role != null) {
            jpql.append(" and a.accountRole = :role");
            params.put("role", role);
        }
        return count(jpql.toString(), params);
    }

    // ---------- Support tickets ----------

    public record TicketRow(SupportTicket ticket, Person creator) {
    }

    /** Admin chỉ thấy phiếu đã gửi (bỏ DRAFT). */
    public Page<TicketRow> searchTickets(String keyword, TicketStatus status, Pageable pageable) {
        StringBuilder from = new StringBuilder(
                " from SupportTicket t, Person p where t.createdPersonId = p.personId and t.ticketStatus <> :draft");
        Map<String, Object> params = new HashMap<>();
        params.put("draft", TicketStatus.DRAFT);
        if (status != null) {
            from.append(" and t.ticketStatus = :status");
            params.put("status", status);
        }
        if (hasText(keyword)) {
            from.append(" and (lower(t.content) like :kw or lower(p.name) like :kw or lower(p.email) like :kw)");
            params.put("kw", like(keyword));
        }
        long total = count("select count(t)" + from, params);
        TypedQuery<Object[]> q = em.createQuery("select t, p" + from + " order by t.supportTicketId desc",
                Object[].class);
        params.forEach(q::setParameter);
        q.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize());
        List<TicketRow> rows = q.getResultList().stream()
                .map(r -> new TicketRow((SupportTicket) r[0], (Person) r[1])).toList();
        return new PageImpl<>(rows, pageable, total);
    }

    public long countTickets(TicketStatus status) {
        return em.createQuery("select count(t) from SupportTicket t where t.ticketStatus = :s", Long.class)
                .setParameter("s", status).getSingleResult();
    }

    public TicketRow findTicket(Long id) {
        List<Object[]> rows = em.createQuery("select t, p from SupportTicket t, Person p"
                        + " where t.createdPersonId = p.personId and t.supportTicketId = :id", Object[].class)
                .setParameter("id", id).getResultList();
        return rows.isEmpty() ? null : new TicketRow((SupportTicket) rows.get(0)[0], (Person) rows.get(0)[1]);
    }

    // ---------- Activity logs ----------

    public record LogRow(ActivityLog log, Person person) {
    }

    public Page<LogRow> searchLogs(String keyword, ActionType action, ActivityStatus status,
                                   LocalDateTime from, LocalDateTime toExclusive, Pageable pageable) {
        StringBuilder body = new StringBuilder(
                " from ActivityLog l left join Person p on l.performedPersonId = p.personId where 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (action != null) {
            body.append(" and l.actionType = :action");
            params.put("action", action);
        }
        if (status != null) {
            body.append(" and l.activityStatus = :status");
            params.put("status", status);
        }
        if (from != null) {
            body.append(" and l.logTime >= :from");
            params.put("from", from);
        }
        if (toExclusive != null) {
            body.append(" and l.logTime < :to");
            params.put("to", toExclusive);
        }
        if (hasText(keyword)) {
            body.append(" and (lower(coalesce(l.activityDescription, '')) like :kw"
                    + " or lower(coalesce(p.name, '')) like :kw or lower(coalesce(p.email, '')) like :kw"
                    + " or lower(coalesce(l.ipAddress, '')) like :kw");
            params.put("kw", like(keyword));
            Long id = parseLogId(keyword);
            if (id != null) {
                body.append(" or l.logId = :logId");
                params.put("logId", id);
            }
            body.append(")");
        }
        long total = count("select count(l)" + body, params);
        TypedQuery<Object[]> q = em.createQuery("select l, p" + body + " order by l.logTime desc, l.logId desc",
                Object[].class);
        params.forEach(q::setParameter);
        q.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize());
        List<LogRow> rows = q.getResultList().stream()
                .map(r -> new LogRow((ActivityLog) r[0], (Person) r[1])).toList();
        return new PageImpl<>(rows, pageable, total);
    }

    public long countLogs(ActivityStatus status) {
        if (status == null) {
            return em.createQuery("select count(l) from ActivityLog l", Long.class).getSingleResult();
        }
        return em.createQuery("select count(l) from ActivityLog l where l.activityStatus = :s", Long.class)
                .setParameter("s", status).getSingleResult();
    }

    public long countLogsSince(ActionType action, ActivityStatus status, LocalDateTime since) {
        return em.createQuery("select count(l) from ActivityLog l where l.actionType = :a"
                        + " and l.activityStatus = :s and l.logTime >= :since", Long.class)
                .setParameter("a", action).setParameter("s", status).setParameter("since", since)
                .getSingleResult();
    }

    /** Thời điểm các lần Register thành công / Login thất bại kể từ `since` (gộp theo ngày ở tầng Service). */
    public List<LogTimes> logTimesForChart(LocalDateTime since) {
        List<Object[]> rows = em.createQuery("select l.actionType, l.activityStatus, l.logTime from ActivityLog l"
                        + " where l.logTime >= :since and ((l.actionType = :reg and l.activityStatus = :ok)"
                        + " or (l.actionType = :login and l.activityStatus = :fail))", Object[].class)
                .setParameter("since", since)
                .setParameter("reg", ActionType.REGISTER).setParameter("ok", ActivityStatus.SUCCESS)
                .setParameter("login", ActionType.LOGIN).setParameter("fail", ActivityStatus.FAILED)
                .getResultList();
        List<LogTimes> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new LogTimes((ActionType) r[0], (ActivityStatus) r[1], (LocalDateTime) r[2]));
        }
        return out;
    }

    public record LogTimes(ActionType actionType, ActivityStatus status, LocalDateTime time) {
    }

    // ---------- Categories (đếm danh mục chạm ngưỡng cảnh báo) ----------

    public record CategoryThreshold(Long categoryId, java.math.BigDecimal warning) {
    }

    public List<CategoryThreshold> categoryWarnings() {
        return em.createQuery("select c.financialCategoryId, c.amountOfWarning from FinancialCategory c",
                        Object[].class).getResultList().stream()
                .map(r -> new CategoryThreshold((Long) r[0], (java.math.BigDecimal) r[1])).toList();
    }

    /** categoryId -> tổng Expense đã APPROVED (số tiền "đã sử dụng" theo nghiệp vụ). */
    public Map<Long, java.math.BigDecimal> approvedExpenseByCategory() {
        Map<Long, java.math.BigDecimal> out = new HashMap<>();
        em.createQuery("select t.financialCategoryId, sum(t.amount) from Transaction t"
                                + " where t.transactionType = :type and t.transactionStatus = :status"
                                + " group by t.financialCategoryId", Object[].class)
                .setParameter("type", ute.fit.financemanagement.enums.TransactionType.EXPENSE)
                .setParameter("status", ute.fit.financemanagement.enums.TransactionStatus.APPROVED)
                .getResultList()
                .forEach(r -> out.put((Long) r[0], (java.math.BigDecimal) r[1]));
        return out;
    }

    // ---------- helpers ----------

    private long count(String jpql, Map<String, Object> params) {
        TypedQuery<Long> q = em.createQuery(jpql, Long.class);
        params.forEach(q::setParameter);
        return q.getSingleResult();
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String like(String keyword) {
        return "%" + keyword.trim().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
    }

    private static Long parseLogId(String keyword) {
        String k = keyword.trim().replaceFirst("(?i)^#?(log-)?", "");
        try {
            return Long.valueOf(k);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
