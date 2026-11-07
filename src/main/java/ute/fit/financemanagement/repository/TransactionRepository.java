package ute.fit.financemanagement.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ute.fit.financemanagement.entity.Transaction;
import ute.fit.financemanagement.enums.TransactionStatus;
import ute.fit.financemanagement.enums.TransactionType;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByFinancialCategoryIdIn(Collection<Long> categoryIds, Pageable pageable);

    List<Transaction> findByFinancialCategoryIdInAndTransactionStatusIn(
            Collection<Long> categoryIds, Collection<TransactionStatus> statuses);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t "
            + "where t.financialCategoryId in :ids and t.transactionType = :type "
            + "and t.transactionStatus = :status")
    BigDecimal sumAmount(@Param("ids") Collection<Long> categoryIds,
                         @Param("type") TransactionType type,
                         @Param("status") TransactionStatus status);
}