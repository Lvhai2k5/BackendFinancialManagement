package ute.fit.financemanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ute.fit.financemanagement.enums.TicketStatus;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long supportTicketId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(columnDefinition = "text")
    private String noteOfRespondent;

    @Column(nullable = false)
    private Long createdPersonId;

    private Long answeredPersonId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TicketStatus ticketStatus = TicketStatus.DRAFT;

    public void handleWithTicket(Long answeredPersonId, String noteOfRespondent) {
        if (ticketStatus == TicketStatus.DRAFT || ticketStatus == TicketStatus.RESPONDED) {
            throw new IllegalStateException("Ticket chưa gửi hoặc đã phản hồi, trạng thái: " + ticketStatus);
        }
        this.answeredPersonId = answeredPersonId;
        this.noteOfRespondent = noteOfRespondent;
        this.ticketStatus = TicketStatus.RESPONDED;
    }

    public void changeTicketStatus(TicketStatus newStatus) {
        if (newStatus.ordinal() != ticketStatus.ordinal() + 1) {
            throw new IllegalStateException(
                    "Không thể chuyển ticket từ " + ticketStatus + " sang " + newStatus);
        }
        this.ticketStatus = newStatus;
    }
}
