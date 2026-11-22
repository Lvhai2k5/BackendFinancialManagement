package ute.fit.financemanagement.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ute.fit.financemanagement.dto.request.FamilyRequests;
import ute.fit.financemanagement.dto.response.FamilyResponses;
import ute.fit.financemanagement.dto.response.OverviewResponse;
import ute.fit.financemanagement.security.CurrentUser;
import ute.fit.financemanagement.service.FamilyService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Families")
@SecurityRequirement(name = "bearerAuth")
public class FamilyController {

    private final FamilyService familyService;

    @GetMapping("/families")
    @Operation(summary = "Liệt kê gia đình người dùng tham gia")
    public List<FamilyResponses.Summary> list() {
        return familyService.listMine(CurrentUser.get().personId());
    }

    @GetMapping("/families/{familyId}")
    @Operation(summary = "Xem chi tiết gia đình, thành viên và hạng mục")
    public FamilyResponses.Detail detail(@PathVariable Long familyId) {
        return familyService.detail(familyId, CurrentUser.get().personId());
    }

    @GetMapping("/families/{familyId}/overview")
    @Operation(summary = "Lấy tổng quan thu chi, danh mục và 10 giao dịch gần nhất")
    public OverviewResponse overview(@PathVariable Long familyId) {
        return familyService.overview(familyId, CurrentUser.get().personId());
    }

    @PostMapping("/families")
    @Operation(summary = "Tạo gia đình")
    public ResponseEntity<FamilyResponses.Detail> create(@Valid @RequestBody FamilyRequests.CreateFamily request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(familyService.create(request, CurrentUser.get().personId()));
    }

    /** "Nhập mã mời": tham gia gia đình bằng mã `#FN-<id>`. */
    @PostMapping("/families/join")
    @Operation(summary = "Tham gia gia đình bằng mã mời")
    public FamilyResponses.Detail join(@Valid @RequestBody FamilyRequests.JoinFamily request) {
        return familyService.join(request, CurrentUser.get().personId());
    }

    @PostMapping("/families/{familyId}/members")
    @Operation(summary = "Thêm thành viên vào gia đình")
    public ResponseEntity<FamilyResponses.Detail> addMember(
            @PathVariable Long familyId, @Valid @RequestBody FamilyRequests.AddMember request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(familyService.addMember(familyId, request, CurrentUser.get().personId()));
    }

    @DeleteMapping("/families/{familyId}/members/{memberId}")
    @Operation(summary = "Xóa thành viên khỏi gia đình")
    public FamilyResponses.Detail removeMember(@PathVariable Long familyId, @PathVariable Long memberId) {
        return familyService.removeMember(familyId, memberId, CurrentUser.get().personId());
    }

    @PutMapping("/families/{familyId}/members/{memberId}/permissions")
    @Operation(summary = "Cập nhật quyền thành viên")
    public FamilyResponses.Detail updatePermissions(
            @PathVariable Long familyId, @PathVariable Long memberId,
            @Valid @RequestBody FamilyRequests.UpdatePermissions request) {
        return familyService.updatePermissions(familyId, memberId, request, CurrentUser.get().personId());
    }

    @PostMapping("/families/{familyId}/members/{memberId}/transfer-ownership")
    @Operation(summary = "Chuyển quyền chủ hộ cho thành viên")
    public FamilyResponses.Detail transferOwnership(@PathVariable Long familyId, @PathVariable Long memberId) {
        return familyService.transferOwnership(familyId, memberId, CurrentUser.get().personId());
    }

    @PostMapping("/families/{familyId}/financial-categories")
    @Operation(summary = "Tạo hạng mục tài chính và các ngưỡng")
    public ResponseEntity<FamilyResponses.Category> createCategory(
            @PathVariable Long familyId, @Valid @RequestBody FamilyRequests.CreateCategory request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(familyService.createCategory(familyId, request, CurrentUser.get().personId()));
    }

    @PutMapping("/financial-categories/{categoryId}/thresholds")
    @Operation(summary = "Cập nhật ngưỡng cảnh báo, khẩn cấp và phê duyệt")
    public FamilyResponses.Category updateThreshold(
            @PathVariable Long categoryId, @Valid @RequestBody FamilyRequests.UpdateThreshold request) {
        return familyService.updateThreshold(categoryId, request, CurrentUser.get().personId());
    }

    @PostMapping("/financial-categories/{categoryId}/lock")
    @Operation(summary = "Khóa hạng mục tài chính")
    public FamilyResponses.Category lock(@PathVariable Long categoryId) {
        return familyService.setLocked(categoryId, true, CurrentUser.get().personId());
    }

    @PostMapping("/financial-categories/{categoryId}/unlock")
    @Operation(summary = "Mở khóa hạng mục tài chính")
    public FamilyResponses.Category unlock(@PathVariable Long categoryId) {
        return familyService.setLocked(categoryId, false, CurrentUser.get().personId());
    }
}
