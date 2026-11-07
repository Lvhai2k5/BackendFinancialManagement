package ute.fit.financemanagement.service;

import java.time.LocalDate;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.AccountStats;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.AccountSummary;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.ActivityLogItem;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.ActivityLogStats;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.AdminTicket;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.Dashboard;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.PageResponse;
import ute.fit.financemanagement.dto.response.admin.AdminResponses.TicketCounts;
import ute.fit.financemanagement.enums.AccountRole;
import ute.fit.financemanagement.enums.AccountStatus;
import ute.fit.financemanagement.enums.ActionType;
import ute.fit.financemanagement.enums.ActivityStatus;
import ute.fit.financemanagement.enums.TicketStatus;

/** Nghiệp vụ khu vực quản trị Web. Mọi method yêu cầu người gọi có role ADMIN (kiểm tra ở tầng Service). */
public interface AdminService {

    Dashboard dashboard(int days);

    PageResponse<AccountSummary> listAccounts(String keyword, AccountRole role, AccountStatus status,
                                              int page, int size);

    AccountStats accountStats();

    AccountSummary lockAccount(Long accountId, String ip);

    AccountSummary unlockAccount(Long accountId, String ip);

    PageResponse<AdminTicket> listTickets(String keyword, TicketStatus status, int page, int size);

    TicketCounts ticketCounts();

    AdminTicket markTicketViewed(Long ticketId, String ip);

    AdminTicket respondTicket(Long ticketId, String noteOfRespondent, String ip);

    PageResponse<ActivityLogItem> listLogs(String keyword, ActionType action, ActivityStatus status,
                                           LocalDate from, LocalDate to, int page, int size);

    ActivityLogStats logStats();
}
