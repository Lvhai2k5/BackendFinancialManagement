package ute.fit.financemanagement.service;

import java.util.List;
import ute.fit.financemanagement.dto.request.FamilyRequests;
import ute.fit.financemanagement.dto.response.FamilyResponses;

public interface FamilyService {

    List<FamilyResponses.Summary> listMine(Long personId);

    FamilyResponses.Detail detail(Long familyId, Long personId);

    FamilyResponses.Detail create(FamilyRequests.CreateFamily request, Long personId);

    FamilyResponses.Detail join(FamilyRequests.JoinFamily request, Long personId);

    FamilyResponses.Detail addMember(Long familyId, FamilyRequests.AddMember request, Long personId);

    FamilyResponses.Detail removeMember(Long familyId, Long memberId, Long personId);

    FamilyResponses.Detail updatePermissions(Long familyId, Long memberId,
                                             FamilyRequests.UpdatePermissions request, Long personId);

    FamilyResponses.Detail transferOwnership(Long familyId, Long memberId, Long personId);

    FamilyResponses.Category createCategory(Long familyId, FamilyRequests.CreateCategory request, Long personId);

    FamilyResponses.Category updateThreshold(Long categoryId, FamilyRequests.UpdateThreshold request, Long personId);

    FamilyResponses.Category setLocked(Long categoryId, boolean locked, Long personId);
}