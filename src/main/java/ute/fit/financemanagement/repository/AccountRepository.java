package ute.fit.financemanagement.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ute.fit.financemanagement.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUsernameIgnoreCase(String username);

    Optional<Account> findByPersonId(Long personId);

    boolean existsByUsernameIgnoreCase(String username);
}