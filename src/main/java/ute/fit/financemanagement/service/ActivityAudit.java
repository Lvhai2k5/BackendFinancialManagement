package ute.fit.financemanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ute.fit.financemanagement.entity.ActivityLog;
import ute.fit.financemanagement.enums.ActionType;
import ute.fit.financemanagement.enums.ActivityStatus;
import ute.fit.financemanagement.repository.ActivityLogRepository;

@Service
@RequiredArgsConstructor
public class ActivityAudit {
    private final ActivityLogRepository repository;

    public void record(ActionType action, Long personId, String description) {
        save(action, ActivityStatus.SUCCESS, personId, description, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failedLogin(Long personId, String ip) {
        save(ActionType.LOGIN, ActivityStatus.FAILED, personId, "Đăng nhập thất bại", ip);
    }

    private void save(ActionType action, ActivityStatus status, Long personId, String description, String ip) {
        repository.save(ActivityLog.builder().actionType(action).activityStatus(status)
                .performedPersonId(personId).activityDescription(description).ipAddress(ip).build());
    }
}
