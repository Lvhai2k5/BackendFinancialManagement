package ute.fit.financemanagement.dto.response;

public record UserResponse(Long personId, String username, String name, String email,
                           String phone, String avatar, String role) {
}