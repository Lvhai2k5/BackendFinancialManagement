package ute.fit.financemanagement.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Khớp `family_model.dart` ở Mobile (tên field giữ nguyên). */
public final class FamilyResponses {

    private FamilyResponses() {
    }

    /** role: OWNER | MEMBER */
    public record Member(Long personId, String name, String role) {
    }

    public record Summary(Long familyId, String code, String name, String myRole,
                          BigDecimal totalBudget, int categoryCount, List<Member> members) {
    }

    /** status: ACTIVE | LOCKED. `limitAmount` = số dư/định mức ban đầu của hũ. */
    public record Category(Long financialCategoryId, String name, int allocationPercent,
                           BigDecimal spent, BigDecimal limitAmount, String status,
                           BigDecimal amountOfWarning, BigDecimal amountOfEmergency,
                           BigDecimal amountOfApproval) {
    }

    public record Detail(Long familyId, String code, String name, String myRole,
                         BigDecimal totalBudget, double budgetGrowthPercent, String foundedLabel,
                         List<Member> members, List<Category> categories,
                         BigDecimal totalIncome, BigDecimal totalExpense) {
    }
}