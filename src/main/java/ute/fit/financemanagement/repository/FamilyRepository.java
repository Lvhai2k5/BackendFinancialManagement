package ute.fit.financemanagement.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ute.fit.financemanagement.entity.Family;

public interface FamilyRepository extends JpaRepository<Family, Long> {

    @Query("select distinct f from Family f join f.memberships m where m.personId = :personId order by f.familyId")
    List<Family> findAllByMember(@Param("personId") Long personId);
}