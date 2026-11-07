package ute.fit.financemanagement.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ute.fit.financemanagement.entity.Person;

public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}