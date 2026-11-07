package ute.fit.financemanagement.dto.response.admin;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * DTO response cho khu vực quản trị (Web). Field khớp Frontend-Web/src/types/*.ts.
 * Enum trả về dạng tên enum (ADMIN, ACTIVE, SUBMITTED...) giống phần Mobile.
 */
public final class AdminResponses {

    private AdminResponses() {
    }

    /** Shape giống Spring Data Page (TECH-API.md mục 4) nhưng tự định nghĩa để JSON ổn định. */
    public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int number, int size) {
        public static <T> PageResponse<T> of(Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getTotalPages(),
                    page.getNumber(), page.getSize());
        }
    }

    public record AccountFamilyInfo(Long familyId, String name, int memberCount, String familyMemberRole) {
    }

    public record AccountSummary(Long accountId, Long personId, String username, String name, String email,
                                 String phone, String avatar, String accountRole, String accountStatus,
                                 List<AccountFamilyInfo> families) {
    }

    public record AccountStats(long total, long active, long inactive, long admins) {
    }

    public record TicketCounts(long all, long submitted, long viewed, long responded) {
    }

    /** `createdAt` hiện luôn null: entity SupportTicket chưa lưu thời điểm tạo. */
    public record AdminTicket(Long supportTicketId, String content, String noteOfRespondent,
                              Long createdPersonId, Long answeredPersonId, String ticketStatus,
                              String createdPersonName, String createdPersonEmail, LocalDateTime createdAt) {
    }

    public record ActivityLogItem(Long logId, String actionType, String activityStatus,
                                  String activityDescription, String ipAddress, LocalDateTime logTime,
                                  Long performedPersonId, String performedPersonName,
                                  String performedPersonEmail) {
    }

    public record ActivityLogStats(long total, long success, long failed) {
    }

    public record ChartPoint(String date, long newAccounts, long failedLogins) {
    }

    public record Dashboard(AccountStats accounts, TicketCounts tickets, long warningCategories,
                            long failedLogins24h, List<ChartPoint> chart, List<ActivityLogItem> recentLogs) {
    }
}
