package ma.clubify.common.security;

import ma.clubify.platform.model.entity.Role;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ma.clubify.platform.model.entity.Role.ACCOUNT_ADMIN;
import static ma.clubify.platform.model.entity.Role.ACCOUNTANT;
import static ma.clubify.platform.model.entity.Role.COACH;
import static ma.clubify.platform.model.entity.Role.FRONT_DESK;
import static ma.clubify.platform.model.entity.Role.MANAGER;

/**
 * Catalogue des permissions de F01 et jeu par défaut de chaque rôle
 * (décision 0028, fiche F01 règle 11b).
 *
 * <p>Le catalogue vit en code, pas en base : c'est ce qui garantit qu'une
 * feature déclare ses permissions, et le test d'architecture le vérifie. Seules
 * les surcharges par utilisateur sont stockées.
 *
 * <p>Nommage {@code domaine.objet.action}, actions fermées. Un droit par
 * décision qui engage de l'argent, une personne ou une donnée sensible ; un
 * droit par écran ou liste ; jamais un droit par champ.
 */
public final class Permissions {

    public static final String CLUB_SETTINGS_CONSULTER = "club.settings.consulter";
    public static final String CLUB_SETTINGS_MODIFIER = "club.settings.modifier";

    public static final String USERS_CONSULTER = "users.consulter";
    public static final String USERS_CREER = "users.creer";
    public static final String USERS_MODIFIER = "users.modifier";
    public static final String USERS_DESACTIVER = "users.desactiver";
    /** Jamais délégable : seul l'administrateur du compte ajuste les droits. */
    public static final String USERS_PERMISSIONS_MODIFIER = "users.permissions.modifier";
    public static final String USERS_SESSIONS_FERMER = "users.sessions.fermer";
    public static final String USERS_MFA_REINITIALISER = "users.mfa.reinitialiser";
    public static final String USERS_EXPORTER = "users.exporter";

    public static final String AUDIT_CONSULTER = "audit.consulter";

    public static final String FILES_DEPOSER = "files.deposer";
    public static final String FILES_CONSULTER = "files.consulter";

    /** Permissions qu'aucune surcharge ne peut accorder à un autre rôle. */
    public static final Set<String> NON_DELEGABLES = Set.of(USERS_PERMISSIONS_MODIFIER);

    private static final Map<Role, Set<String>> PAR_ROLE = new EnumMap<>(Role.class);

    static {
        // L'administrateur détient tout, par construction : voir detientTout().
        PAR_ROLE.put(ACCOUNT_ADMIN, Set.of());

        PAR_ROLE.put(MANAGER, Set.of(
                CLUB_SETTINGS_CONSULTER, CLUB_SETTINGS_MODIFIER,
                USERS_CONSULTER, USERS_EXPORTER,
                AUDIT_CONSULTER,
                FILES_DEPOSER, FILES_CONSULTER));

        PAR_ROLE.put(FRONT_DESK, Set.of(
                CLUB_SETTINGS_CONSULTER,
                USERS_EXPORTER,
                FILES_DEPOSER, FILES_CONSULTER));

        PAR_ROLE.put(COACH, Set.of());

        PAR_ROLE.put(ACCOUNTANT, Set.of(CLUB_SETTINGS_CONSULTER));

        PAR_ROLE.put(Role.PARENT, Set.of());
    }

    /** Toutes les permissions déclarées par F01. */
    public static final List<String> CATALOGUE = List.of(
            CLUB_SETTINGS_CONSULTER, CLUB_SETTINGS_MODIFIER,
            USERS_CONSULTER, USERS_CREER, USERS_MODIFIER, USERS_DESACTIVER,
            USERS_PERMISSIONS_MODIFIER, USERS_SESSIONS_FERMER, USERS_MFA_REINITIALISER,
            USERS_EXPORTER, AUDIT_CONSULTER, FILES_DEPOSER, FILES_CONSULTER);

    /** Le jeu par défaut du rôle, avant toute surcharge. */
    public static Set<String> parDefaut(Role role) {
        return detientTout(role) ? Set.copyOf(CATALOGUE) : PAR_ROLE.getOrDefault(role, Set.of());
    }

    /**
     * L'administrateur du compte détient tout, et ses droits ne se retirent pas
     * (décision 0028, critère C12b).
     */
    public static boolean detientTout(Role role) {
        return role == ACCOUNT_ADMIN;
    }

    public static boolean estDelegable(String permission) {
        return !NON_DELEGABLES.contains(permission);
    }

    private Permissions() {
    }
}
