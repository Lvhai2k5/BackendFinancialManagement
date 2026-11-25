package ute.fit.financemanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class VisionRequests {

    private VisionRequests() {
    }

    /** Ảnh đồ vật đã nén ở thiết bị (base64, không tiền tố data:); Backend chỉ nhận diện, không tạo giao dịch. */
    public record ScanItem(
            @NotNull(message = "Thiếu gia đình") Long familyId,
            @NotBlank(message = "Thiếu kiểu ảnh")
            @Pattern(regexp = "image/(jpeg|png|webp)", message = "Chỉ hỗ trợ ảnh JPEG, PNG hoặc WebP") String mimeType,
            @NotBlank(message = "Ảnh không được để trống")
            @Size(max = 6_000_000, message = "Ảnh quá lớn, vui lòng chụp lại") String imageBase64) {
    }
}
