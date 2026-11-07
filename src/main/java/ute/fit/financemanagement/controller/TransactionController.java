package ute.fit.financemanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ute.fit.financemanagement.dto.request.TransactionRequests;
import ute.fit.financemanagement.dto.response.TransactionResponse;
import ute.fit.financemanagement.security.CurrentUser;
import ute.fit.financemanagement.service.TransactionService;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /** Phân trang theo TECH-API.md: `?page=0&size=20&sort=createdAt,desc`. */
    @GetMapping
    public Page<TransactionResponse> list(
            @RequestParam(required = false) Long familyId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return transactionService.list(CurrentUser.get().personId(), familyId, pageable);
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequests.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.create(request, CurrentUser.get().personId()));
    }

    @PostMapping("/{transactionId}/approve")
    public TransactionResponse approve(@PathVariable Long transactionId) {
        return transactionService.approve(transactionId, CurrentUser.get().personId());
    }

    @PostMapping("/{transactionId}/reject")
    public TransactionResponse reject(@PathVariable Long transactionId) {
        return transactionService.reject(transactionId, CurrentUser.get().personId());
    }
}