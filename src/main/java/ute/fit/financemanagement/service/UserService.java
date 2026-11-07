package ute.fit.financemanagement.service;

import java.util.List;
import ute.fit.financemanagement.dto.request.UserRequests;
import ute.fit.financemanagement.dto.response.SupportTicketResponse;
import ute.fit.financemanagement.dto.response.UserResponse;

public interface UserService {

    UserResponse me(Long personId);

    UserResponse updateMe(Long personId, UserRequests.UpdateProfile request);

    SupportTicketResponse createTicket(Long personId, UserRequests.CreateSupportTicket request);

    List<SupportTicketResponse> myTickets(Long personId);
}