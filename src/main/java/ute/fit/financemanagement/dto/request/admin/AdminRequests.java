package ute.fit.financemanagement.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AdminRequests {

    private AdminRequests() {
    }

    /** Phản hồi phiếu hỗ trợ: bắt buộc noteOfRespondent (BUSINESS-REQUIREMENTS 3.5). */
    public record RespondTicket(@NotBlank(message = "Nội dung phản hồi không được để trống")
                                @Size(max = 5000, message = "Nội dung phản hồi tối đa 5000 ký tự")
                                String noteOfRespondent) {
    }
}
