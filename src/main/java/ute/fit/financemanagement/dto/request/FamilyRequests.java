package ute.fit.financemanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import ute.fit.financemanagement.enums.FamilyPermission;

public final class FamilyRequests {

    private FamilyRequests() {
    }

    public record CreateFamily(@NotBlank(message = "Tên gia đình không được để trống") String name) {
    }

    /** `code` dạng "#FN-204" hoặc "204". */
    public record JoinFamily(@NotBlank(message = "Mã gia đình không được để trống") String code) {
    }

    /** Thêm thành viên bằng email (ưu tiên) hoặc tên đăng nhập; cần ít nhất một trong hai. */
    public record AddMember(String email, String username) {
    }

    public record UpdatePermissions(
            @NotNull(message = "Thiếu danh sách quyền") List<FamilyPermission> permissions) {
    }

    public record CreateCategory(
            @NotBlank(message = "Tên danh mục không được để trống") String name,
            String description,
            @NotNull(message = "Số dư ban đầu không được để trống")
            @PositiveOrZero(message = "Số dư ban đầu không được âm") BigDecimal initialBalance,
            @NotNull @PositiveOrZero BigDecimal amountOfWarning,
            @NotNull @PositiveOrZero BigDecimal amountOfEmergency,
            @NotNull @PositiveOrZero BigDecimal amountOfApproval) {
    }

    public record UpdateThreshold(
            @NotNull @PositiveOrZero BigDecimal amountOfWarning,
            @NotNull @PositiveOrZero BigDecimal amountOfEmergency,
            @NotNull @PositiveOrZero BigDecimal amountOfApproval) {
    }
}