package ute.fit.financemanagement.dto.response;

/**
 * kind: APPROVAL_REQUEST | THRESHOLD_WARNING | SYNC_SUCCESS.
 * `id` của APPROVAL_REQUEST chính là transactionId để Mobile gọi approve/reject.
 */
public record NotificationResponse(Long id, String kind, String title, String message,
                                   String timeLabel, boolean actionable) {
}