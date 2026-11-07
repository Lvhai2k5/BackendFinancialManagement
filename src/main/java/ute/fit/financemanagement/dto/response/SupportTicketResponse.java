package ute.fit.financemanagement.dto.response;

public record SupportTicketResponse(Long supportTicketId, String content, String status,
                                    String noteOfRespondent) {
}