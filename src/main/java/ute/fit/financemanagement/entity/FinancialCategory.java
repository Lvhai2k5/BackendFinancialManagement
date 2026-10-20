package ute.fit.financemanagement.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ute.fit.financemanagement.enums.CategoryStatus;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinancialCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long financialCategoryId;

    @Column(nullable = false)
    private Long familyId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal initialBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryStatus categoryStatus;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal amountOfWarning;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal amountOfEmergency;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal amountOfApproval;

    private FinancialCategory(Long familyId, String name, String description,
                              BigDecimal initialBalance, BigDecimal amountOfWarning,
                              BigDecimal amountOfEmergency, BigDecimal amountOfApproval) {
        this.familyId = familyId;
        this.name = name;
        this.description = description;
        this.initialBalance = initialBalance;
        this.categoryStatus = CategoryStatus.ACTIVE;
        applyThreshold(amountOfWarning, amountOfEmergency, amountOfApproval);
    }

    static FinancialCategory create(Long familyId, String name, String description,
                                    BigDecimal initialBalance, BigDecimal amountOfWarning,
                                    BigDecimal amountOfEmergency, BigDecimal amountOfApproval) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống");
        }
        requireNotNegative(initialBalance, "Số dư ban đầu");
        return new FinancialCategory(familyId, name, description, initialBalance,
                amountOfWarning, amountOfEmergency, amountOfApproval);
    }

    public void lockCategory() {
        this.categoryStatus = CategoryStatus.LOCKED;
    }

    public void unlockCategory() {
        this.categoryStatus = CategoryStatus.ACTIVE;
    }

    public void modifyThreshold(BigDecimal amountOfWarning, BigDecimal amountOfEmergency,
                                BigDecimal amountOfApproval) {
        applyThreshold(amountOfWarning, amountOfEmergency, amountOfApproval);
    }

    private void applyThreshold(BigDecimal warning, BigDecimal emergency, BigDecimal approval) {
        requireNotNegative(warning, "Ngưỡng cảnh báo");
        requireNotNegative(emergency, "Ngưỡng khẩn cấp");
        requireNotNegative(approval, "Ngưỡng cần duyệt");
        this.amountOfWarning = warning;
        this.amountOfEmergency = emergency;
        this.amountOfApproval = approval;
    }

    private static void requireNotNegative(BigDecimal value, String label) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(label + " không được âm");
        }
    }
}
