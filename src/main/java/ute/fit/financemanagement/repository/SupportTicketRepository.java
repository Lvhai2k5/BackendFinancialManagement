package ute.fit.financemanagement.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ute.fit.financemanagement.entity.SupportTicket;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByCreatedPersonIdOrderBySupportTicketIdDesc(Long createdPersonId);
}