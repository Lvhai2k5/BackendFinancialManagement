package ute.fit.financemanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserRequests {

    private UserRequests() {
    }

    public record UpdateProfile(
            @NotBlank(message = "Họ tên không được để trống") String name,
            String phone,
            String email) {
    }

    public record ChangePassword(
            @NotBlank(message = "Mật khẩu hiện tại không được để trống") String currentPassword,
            @NotBlank(message = "Mật khẩu mới không được để trống")
            @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự") String newPassword) {
    }

    public record CreateSupportTicket(
            @NotBlank(message = "Nội dung phản ánh không được để trống") String content) {
    }
}
