package ute.fit.financemanagement.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ute.fit.financemanagement.entity.FinancialCategory;

public interface FinancialCategoryRepository extends JpaRepository<FinancialCategory, Long> {

    List<FinancialCategory> findByFamilyIdOrderByFinancialCategoryId(Long familyId);

    List<FinancialCategory> findByFamilyIdIn(Collection<Long> familyIds);

    @Query("select c.familyId from FinancialCategory c where c.financialCategoryId = :categoryId")
    Optional<Long> findFamilyId(@Param("categoryId") Long categoryId);
}
