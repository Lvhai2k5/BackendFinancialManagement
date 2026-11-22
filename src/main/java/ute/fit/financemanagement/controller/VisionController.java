package ute.fit.financemanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ute.fit.financemanagement.dto.request.VisionRequests;
import ute.fit.financemanagement.dto.response.VoiceResponses;
import ute.fit.financemanagement.security.CurrentUser;
import ute.fit.financemanagement.service.VisionScanService;

@RestController
@RequestMapping("/api/v1/vision")
@RequiredArgsConstructor
public class VisionController {

    private final VisionScanService visionScanService;

    /** Chỉ trả gợi ý điền form (200), không tạo tài nguyên nên không dùng 201. */
    @PostMapping("/scan-item")
    public VoiceResponses.Parsed scanItem(@Valid @RequestBody VisionRequests.ScanItem request) {
        return visionScanService.scanItem(request, CurrentUser.get().personId());
    }
}
