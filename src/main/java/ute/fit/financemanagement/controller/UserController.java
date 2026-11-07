package ute.fit.financemanagement.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ute.fit.financemanagement.dto.request.UserRequests;
import ute.fit.financemanagement.dto.response.SupportTicketResponse;
import ute.fit.financemanagement.dto.response.UserResponse;
import ute.fit.financemanagement.security.CurrentUser;
import ute.fit.financemanagement.service.UserService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/users/me")
    public UserResponse me() {
        return userService.me(CurrentUser.get().personId());
    }

    @PutMapping("/users/me")
    public UserResponse updateMe(@Valid @RequestBody UserRequests.UpdateProfile request) {
        return userService.updateMe(CurrentUser.get().personId(), request);
    }

    /** Màn "Gửi phản ánh & khiếu nại" của Mobile. */
    @PostMapping("/support-tickets")
    public ResponseEntity<SupportTicketResponse> createTicket(
            @Valid @RequestBody UserRequests.CreateSupportTicket request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createTicket(CurrentUser.get().personId(), request));
    }

    @GetMapping("/support-tickets")
    public List<SupportTicketResponse> myTickets() {
        return userService.myTickets(CurrentUser.get().personId());
    }
}