package ute.fit.financemanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ute.fit.financemanagement.entity.Family;
import ute.fit.financemanagement.entity.Membership;
import ute.fit.financemanagement.enums.FamilyMemberRole;
import ute.fit.financemanagement.enums.FamilyPermission;
import ute.fit.financemanagement.exception.ApiException;
import ute.fit.financemanagement.repository.FamilyRepository;

/**
 * Quyền gắn với từng gia đình cụ thể (TECH-AUTH.md mục 4): 1 người có thể là Owner ở
 * gia đình này nhưng Member ở gia đình khác, nên kiểm tra ở tầng Service, không dùng role tĩnh.
 */
@Component
@RequiredArgsConstructor
public class FamilyAccess {

    private final FamilyRepository familyRepository;

    public Family requireFamily(Long familyId) {
        return familyRepository.findById(familyId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy gia đình"));
    }

    public Membership requireMember(Family family, Long personId) {
        return family.getMemberships().stream()
                .filter(m -> m.getPersonId().equals(personId))
                .findFirst()
                .orElseThrow(() -> ApiException.forbidden("Bạn không thuộc gia đình này"));
    }

    public Membership requireOwner(Family family, Long personId) {
        Membership m = requireMember(family, personId);
        if (m.getFamilyMemberRole() != FamilyMemberRole.OWNER) {
            throw ApiException.forbidden("Chỉ Chủ hộ mới được thực hiện thao tác này");
        }
        return m;
    }

    public boolean isOwner(Membership m) {
        return m.getFamilyMemberRole() == FamilyMemberRole.OWNER;
    }

    public boolean canWrite(Membership m) {
        return isOwner(m) || m.getPermissions().contains(FamilyPermission.WRITE);
    }

    public boolean canApprove(Membership m) {
        return isOwner(m) || m.getPermissions().contains(FamilyPermission.APPROVAL);
    }
}