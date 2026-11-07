package ute.fit.financemanagement.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ute.fit.financemanagement.entity.FinancialCategory;

public interface FinancialCategoryRepository extends JpaRepository<FinancialCategory, Long> {

    List<FinancialCategory> findByFamilyIdOrderByFinancialCategoryId(Long familyId);

    List<FinancialCategory> findByFamilyIdIn(Collection<Long> familyIds);
}