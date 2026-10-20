package ute.fit.financemanagement.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ute.fit.financemanagement.enums.FamilyMemberRole;
import ute.fit.financemanagement.enums.FamilyPermission;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"personId", "familyId"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long membershipId;

    @Column(name = "familyId", insertable = false, updatable = false)
    private Long familyId;

    @Column(nullable = false)
    private Long personId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FamilyMemberRole familyMemberRole;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(joinColumns = @JoinColumn(name = "membershipId"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false)
    private List<FamilyPermission> permissions = new ArrayList<>();

    Membership(Long personId, FamilyMemberRole familyMemberRole, List<FamilyPermission> permissions) {
        this.personId = personId;
        this.familyMemberRole = familyMemberRole;
        this.permissions = new ArrayList<>(permissions);
    }

    boolean isOwner() {
        return familyMemberRole == FamilyMemberRole.OWNER;
    }

    void changeRole(FamilyMemberRole newRole) {
        this.familyMemberRole = newRole;
    }

    void replacePermissions(List<FamilyPermission> newPermissions) {
        this.permissions.clear();
        this.permissions.addAll(newPermissions);
    }
}
