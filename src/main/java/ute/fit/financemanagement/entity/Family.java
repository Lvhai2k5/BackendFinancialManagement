package ute.fit.financemanagement.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ute.fit.financemanagement.enums.FamilyMemberRole;
import ute.fit.financemanagement.enums.FamilyPermission;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long familyId;

    @Column(nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "familyId", nullable = false)
    private List<Membership> memberships = new ArrayList<>();

    private Family(String name, Long ownerPersonId) {
        this.name = name;
        this.memberships.add(new Membership(ownerPersonId, FamilyMemberRole.OWNER,
                List.of(FamilyPermission.values())));
    }

    public static Family create(String name, Long ownerPersonId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên gia đình không được để trống");
        }
        if (ownerPersonId == null) {
            throw new IllegalArgumentException("Gia đình phải có Owner");
        }
        return new Family(name, ownerPersonId);
    }

    public List<Membership> getMemberships() {
        return Collections.unmodifiableList(memberships);
    }

    public void addMember(Long personId, List<FamilyPermission> permissions) {
        if (findMembership(personId) != null) {
            throw new IllegalStateException("Người này đã là thành viên của gia đình");
        }
        memberships.add(new Membership(personId, FamilyMemberRole.MEMBER, permissions));
    }

    public void changeFamilyMemberRole(Long personId, FamilyMemberRole newRole) {
        Membership target = requireMembership(personId);
        if (target.isOwner() && newRole != FamilyMemberRole.OWNER && countOwners() == 1) {
            throw new IllegalStateException("Gia đình phải luôn có ít nhất 1 Owner");
        }
        target.changeRole(newRole);
    }

    public void adjustPermission(Long personId, List<FamilyPermission> permissions) {
        requireMembership(personId).replacePermissions(permissions);
    }

    public FinancialCategory createFinancialCategory(String name, String description,
                                                     BigDecimal initialBalance,
                                                     BigDecimal amountOfWarning,
                                                     BigDecimal amountOfEmergency,
                                                     BigDecimal amountOfApproval) {
        if (familyId == null) {
            throw new IllegalStateException("Gia đình phải được lưu trước khi tạo danh mục");
        }
        return FinancialCategory.create(familyId, name, description, initialBalance,
                amountOfWarning, amountOfEmergency, amountOfApproval);
    }

    private Membership findMembership(Long personId) {
        return memberships.stream()
                .filter(m -> m.getPersonId().equals(personId))
                .findFirst()
                .orElse(null);
    }

    private Membership requireMembership(Long personId) {
        Membership m = findMembership(personId);
        if (m == null) {
            throw new IllegalArgumentException("Người này không thuộc gia đình");
        }
        return m;
    }

    private long countOwners() {
        return memberships.stream().filter(Membership::isOwner).count();
    }
}
