package ute.fit.financemanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public final class FamilyRequests {

    private FamilyRequests() {
    }

    public record CreateFamily(@NotBlank(message = "Tên gia đình không được để trống") String name) {
    }

    /** `code` dạng "#FN-204" hoặc "204". */
    public record JoinFamily(@NotBlank(message = "Mã gia đình không được để trống") String code) {
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