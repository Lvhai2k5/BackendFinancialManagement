package ute.fit.financemanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class VoiceRequests {

    private VoiceRequests() {
    }

    /** Văn bản đã được STT trên thiết bị nhận diện; Backend chỉ trích xuất gợi ý, không tạo giao dịch. */
    public record Parse(
            @NotNull(message = "Thiếu gia đình") Long familyId,
            @NotBlank(message = "Nội dung giọng nói không được để trống")
            @Size(max = 500, message = "Nội dung giọng nói quá dài (tối đa 500 ký tự)") String transcript) {
    }
}
