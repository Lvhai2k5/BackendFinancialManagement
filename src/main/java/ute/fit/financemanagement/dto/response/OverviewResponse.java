package ute.fit.financemanagement.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Lifetime overview data for one family. */
public record OverviewResponse(Long familyId, BigDecimal balance, BigDecimal income,
                               BigDecimal expense, List<CategoryProgress> categories,
                               List<TransactionResponse> recentTransactions) {

    public record CategoryProgress(Long financialCategoryId, String name,
                                   BigDecimal spent, BigDecimal limitAmount,
                                   BigDecimal usagePercent, String status,
                                   int allocationPercent, BigDecimal amountOfWarning,
                                   BigDecimal amountOfEmergency) {
    }
}
