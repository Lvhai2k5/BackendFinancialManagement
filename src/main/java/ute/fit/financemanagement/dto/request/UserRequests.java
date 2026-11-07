package ute.fit.financemanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class UserRequests {

    private UserRequests() {
    }

    public record UpdateProfile(
            @NotBlank(message = "Họ tên không được để trống") String name,
            String phone,
            @NotBlank(message = "Email không được để trống")
            @Email(message = "Email không đúng định dạng") String email) {
    }

    public record CreateSupportTicket(
            @NotBlank(message = "Nội dung phản ánh không được để trống") String content) {
    }
}