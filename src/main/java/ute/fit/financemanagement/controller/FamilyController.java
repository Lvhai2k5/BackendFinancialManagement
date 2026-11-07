package ute.fit.financemanagement.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ute.fit.financemanagement.dto.request.FamilyRequests;
import ute.fit.financemanagement.dto.response.FamilyResponses;
import ute.fit.financemanagement.security.CurrentUser;
import ute.fit.financemanagement.service.FamilyService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    @GetMapping("/families")
    public List<FamilyResponses.Summary> list() {
        return familyService.listMine(CurrentUser.get().personId());
    }

    @GetMapping("/families/{familyId}")
    public FamilyResponses.Detail detail(@PathVariable Long familyId) {
        return familyService.detail(familyId, CurrentUser.get().personId());
    }

    @PostMapping("/families")
    public ResponseEntity<FamilyResponses.Detail> create(@Valid @RequestBody FamilyRequests.CreateFamily request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(familyService.create(request, CurrentUser.get().personId()));
    }

    /** "Nhập mã mời": tham gia gia đình bằng mã `#FN-<id>`. */
    @PostMapping("/families/join")
    public FamilyResponses.Detail join(@Valid @RequestBody FamilyRequests.JoinFamily request) {
        return familyService.join(request, CurrentUser.get().personId());
    }

    @PostMapping("/families/{familyId}/financial-categories")
    public ResponseEntity<FamilyResponses.Category> createCategory(
            @PathVariable Long familyId, @Valid @RequestBody FamilyRequests.CreateCategory request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(familyService.createCategory(familyId, request, CurrentUser.get().personId()));
    }

    @PutMapping("/financial-categories/{categoryId}/thresholds")
    public FamilyResponses.Category updateThreshold(
            @PathVariable Long categoryId, @Valid @RequestBody FamilyRequests.UpdateThreshold request) {
        return familyService.updateThreshold(categoryId, request, CurrentUser.get().personId());
    }

    @PostMapping("/financial-categories/{categoryId}/lock")
    public FamilyResponses.Category lock(@PathVariable Long categoryId) {
        return familyService.setLocked(categoryId, true, CurrentUser.get().personId());
    }

    @PostMapping("/financial-categories/{categoryId}/unlock")
    public FamilyResponses.Category unlock(@PathVariable Long categoryId) {
        return familyService.setLocked(categoryId, false, CurrentUser.get().personId());
    }
}