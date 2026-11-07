package ute.fit.financemanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ute.fit.financemanagement.entity.ActivityLog;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
}