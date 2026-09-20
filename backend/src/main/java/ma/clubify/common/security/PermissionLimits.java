package ma.clubify.common.security;

import ma.clubify.config.AuthenticatedUser;
import ma.clubify.platform.repository.UserPermissionOverrideRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Plafond attaché à une permission paramétrée (décision 0028).
 *
 * <p>Le cahier des charges ne dit pas « droit de remise » mais « plafond de
 * remise par rôle » (TAR-05) : un droit peut donc porter une valeur, et non
 * seulement un oui ou un non.
 */
@Component
public class PermissionLimits {

    private final UserPermissionOverrideRepository surcharges;
    private final ObjectMapper json;

    public PermissionLimits(UserPermissionOverrideRepository surcharges, ObjectMapper json) {
        this.surcharges = surcharges;
        this.json = json;
    }

    /** Plafond fixé pour cet utilisateur, ou {@code null} s'il n'y en a pas. */
    public Number plafondDe(AuthenticatedUser utilisateur, String permission) {
        return surcharges.findAllByMembershipId(utilisateur.membershipId()).stream()
                .filter(surcharge -> surcharge.getPermissionCode().equals(permission))
                .filter(surcharge -> surcharge.getParameter() != null)
                .findFirst()
                .map(surcharge -> json.readValue(surcharge.getParameter(), Number.class))
                .orElse(null);
    }
}
