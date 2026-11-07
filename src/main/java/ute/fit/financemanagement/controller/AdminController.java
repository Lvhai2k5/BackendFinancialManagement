package ute.fit.financemanagement.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ute.fit.financemanagement.dto.request.admin.AdminRequests.RespondTicket;
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
import ute.fit.financemanagement.service.AdminService;

/** API cho Web Admin. Phân quyền ADMIN được kiểm tra ở AdminService. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public Dashboard dashboard(@RequestParam(defaultValue = "7") int days) {
        return adminService.dashboard(days);
    }

    // ----- accounts -----

    @GetMapping("/accounts")
    public PageResponse<AccountSummary> accounts(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) AccountRole accountRole,
                                                 @RequestParam(required = false) AccountStatus accountStatus,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return adminService.listAccounts(keyword, accountRole, accountStatus, page, size);
    }

    @GetMapping("/accounts/stats")
    public AccountStats accountStats() {
        return adminService.accountStats();
    }

    @PatchMapping("/accounts/{accountId}/lock")
    public AccountSummary lock(@PathVariable Long accountId, HttpServletRequest request) {
        return adminService.lockAccount(accountId, clientIp(request));
    }

    @PatchMapping("/accounts/{accountId}/unlock")
    public AccountSummary unlock(@PathVariable Long accountId, HttpServletRequest request) {
        return adminService.unlockAccount(accountId, clientIp(request));
    }

    // ----- support tickets -----

    @GetMapping("/support-tickets")
    public PageResponse<AdminTicket> tickets(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) TicketStatus ticketStatus,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return adminService.listTickets(keyword, ticketStatus, page, size);
    }

    @GetMapping("/support-tickets/counts")
    public TicketCounts ticketCounts() {
        return adminService.ticketCounts();
    }

    @PatchMapping("/support-tickets/{ticketId}/view")
    public AdminTicket view(@PathVariable Long ticketId, HttpServletRequest request) {
        return adminService.markTicketViewed(ticketId, clientIp(request));
    }

    @PostMapping("/support-tickets/{ticketId}/respond")
    public ResponseEntity<AdminTicket> respond(@PathVariable Long ticketId,
                                               @Valid @RequestBody RespondTicket body,
                                               HttpServletRequest request) {
        return ResponseEntity.ok(adminService.respondTicket(ticketId, body.noteOfRespondent(), clientIp(request)));
    }

    // ----- activity logs -----

    @GetMapping("/activity-logs")
    public PageResponse<ActivityLogItem> logs(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) ActionType actionType,
                                              @RequestParam(required = false) ActivityStatus activityStatus,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return adminService.listLogs(keyword, actionType, activityStatus, from, to, page, size);
    }

    @GetMapping("/activity-logs/stats")
    public ActivityLogStats logStats() {
        return adminService.logStats();
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        return ip != null && ip.length() > 45 ? ip.substring(0, 45) : ip;
    }
}
