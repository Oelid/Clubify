package ma.clubify.platform.service;

import ma.clubify.common.security.Permissions;
import ma.clubify.platform.model.entity.Membership;
import ma.clubify.platform.model.entity.UserPermissionOverride;
import ma.clubify.platform.repository.MembershipRepository;
import ma.clubify.platform.repository.UserPermissionOverrideRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Permissions effectives d'une appartenance : le jeu du rôle, puis les
 * surcharges (décision 0028).
 *
 * <p>Les droits de l'administrateur du compte ne se retirent pas : aucune
 * surcharge ne s'applique à lui (critère C12b).
 */
@Service
public class PermissionResolver {

    private final UserPermissionOverrideRepository surcharges;
    private final MembershipRepository appartenances;

    public PermissionResolver(UserPermissionOverrideRepository surcharges,
                              MembershipRepository appartenances) {
        this.surcharges = surcharges;
        this.appartenances = appartenances;
    }

    public Set<String> effectives(Membership appartenance) {
        if (Permissions.detientTout(appartenance.getRole())) {
            return Set.copyOf(Permissions.CATALOGUE);
        }

        Set<String> effectives = new HashSet<>(Permissions.parDefaut(appartenance.getRole()));
        for (UserPermissionOverride surcharge : surchargesDe(appartenance)) {
            if (surcharge.isGranted()) {
                effectives.add(surcharge.getPermissionCode());
            } else {
                effectives.remove(surcharge.getPermissionCode());
            }
        }
        return Set.copyOf(effectives);
    }

    public Membership appartenance(java.util.UUID membershipId) {
        return appartenances.findById(membershipId).orElseThrow(
                () -> new ma.clubify.common.exception.NotFoundException("membership.notFound"));
    }

    public Membership appartenanceDe(java.util.UUID userId) {
        return appartenances.findByUserId(userId).orElseThrow(
                () -> new ma.clubify.common.exception.NotFoundException("membership.notFound"));
    }

    public List<UserPermissionOverride> surchargesDe(Membership appartenance) {
        return surcharges.findAllByMembershipId(appartenance.getId());
    }
}
