package ute.fit.financemanagement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ute.fit.financemanagement.dto.request.VoiceRequests;
import ute.fit.financemanagement.dto.response.VoiceResponses;
import ute.fit.financemanagement.security.CurrentUser;
import ute.fit.financemanagement.service.VoiceParseService;

@RestController
@RequestMapping("/api/v1/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceParseService voiceParseService;

    /** Chỉ trả gợi ý điền form (200), không tạo tài nguyên nên không dùng 201. */
    @PostMapping("/parse")
    public VoiceResponses.Parsed parse(@Valid @RequestBody VoiceRequests.Parse request) {
        return voiceParseService.parse(request, CurrentUser.get().personId());
    }
}
