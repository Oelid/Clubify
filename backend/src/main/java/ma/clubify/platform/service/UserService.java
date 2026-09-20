package ma.clubify.platform.service;

import ma.clubify.common.event.DomainEvent;
import ma.clubify.common.event.DomainEvents;
import ma.clubify.common.exception.BusinessRuleException;
import ma.clubify.common.exception.NotFoundException;
import ma.clubify.common.model.entity.UuidV7;
import ma.clubify.common.security.PermissionChecker;
import ma.clubify.common.security.Permissions;
import ma.clubify.config.AuthenticatedUser;
import ma.clubify.platform.model.entity.Membership;
import ma.clubify.platform.model.entity.Role;
import ma.clubify.platform.model.entity.UserAccount;
import ma.clubify.platform.model.entity.UserPermissionOverride;
import ma.clubify.platform.repository.MembershipRepository;
import ma.clubify.platform.repository.UserAccountRepository;
import ma.clubify.platform.repository.UserPermissionOverrideRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestion des utilisateurs du club (SEC-02, décision 0028).
 *
 * <p>Les permissions sont vérifiées ici, dans le service, et non seulement à
 * l'écran : un appel direct à l'API passe par les mêmes contrôles (critère C11).
 */
@Service
public class UserService {

    private final UserAccountRepository comptes;
    private final MembershipRepository appartenances;
    private final UserPermissionOverrideRepository surcharges;
    private final PermissionResolver permissions;
    private final SessionService sessions;
    private final ClubSettingService reglages;
    private final PasswordEncoder motsDePasse;
    private final DomainEvents evenements;
    private final Clock horloge;

    public UserService(UserAccountRepository comptes, MembershipRepository appartenances,
                       UserPermissionOverrideRepository surcharges, PermissionResolver permissions,
                       SessionService sessions, ClubSettingService reglages,
                       PasswordEncoder motsDePasse, DomainEvents evenements, Clock horloge) {
        this.comptes = comptes;
        this.appartenances = appartenances;
        this.surcharges = surcharges;
        this.permissions = permissions;
        this.sessions = sessions;
        this.reglages = reglages;
        this.motsDePasse = motsDePasse;
        this.evenements = evenements;
        this.horloge = horloge;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@perm.a('users.consulter')")
    public Page<Vue> lister(Pageable pagination) {
        List<Vue> vues = appartenances.findAll().stream()
                .map(appartenance -> comptes.findById(appartenance.getUserId())
                        .map(compte -> new Vue(compte, appartenance)))
                .flatMap(Optional::stream)
                .sorted(java.util.Comparator.comparing(v -> v.compte().getLastName()))
                .toList();
        return new org.springframework.data.domain.PageImpl<>(vues, pagination, vues.size());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@perm.a('users.consulter')")
    public Vue lire(UUID userId) {
        return vue(userId);
    }

    @Transactional
    @PreAuthorize("@perm.a('users.creer')")
    public Vue creer(String email, String prenom, String nom, String telephone, String langue,
                     Role role, String motDePasse) {
        if (!role.attribuableEnR1()) {
            throw new BusinessRuleException("user.role.notAssignableYet");
        }
        long longueurMinimale = reglages.entier("security.password.min_length");
        if (motDePasse == null || motDePasse.length() < longueurMinimale) {
            throw new BusinessRuleException("user.password.tooShort");
        }
        if (comptes.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("user.email.alreadyUsed", HttpStatus.CONFLICT);
        }

        UserAccount compte = new UserAccount();
        compte.setId(UuidV7.next());
        compte.setEmail(email);
        compte.setFirstName(prenom);
        compte.setLastName(nom);
        compte.setPhone(telephone);
        compte.setLanguage(langue == null ? "fr" : langue);
        compte.setPasswordHash(motsDePasse.encode(motDePasse));
        compte.setActive(true);
        comptes.save(compte);

        Membership appartenance = new Membership();
        appartenance.setId(UuidV7.next());
        appartenance.setClubId(PermissionChecker.requis().clubId());
        appartenance.setUserId(compte.getId());
        appartenance.setRole(role);
        appartenance.setActive(true);
        appartenances.save(appartenance);

        publier("user.created", compte.getId(), null, resume(compte, role));
        return new Vue(compte, appartenance);
    }

    @Transactional
    @PreAuthorize("@perm.a('users.modifier')")
    public Vue modifier(UUID userId, String prenom, String nom, String telephone, String langue) {
        Vue vue = vue(userId);
        String avant = resume(vue.compte(), vue.appartenance().getRole());

        vue.compte().setFirstName(prenom);
        vue.compte().setLastName(nom);
        vue.compte().setPhone(telephone);
        if (langue != null) {
            vue.compte().setLanguage(langue);
        }

        publier("user.updated", userId, avant, resume(vue.compte(), vue.appartenance().getRole()));
        return vue;
    }

    @Transactional
    @PreAuthorize("@perm.a('users.desactiver')")
    public void definirStatut(UUID userId, boolean actif) {
        Vue vue = vue(userId);
        if (!actif) {
            interdireSiDernierAdministrateur(vue);
        }

        vue.compte().setActive(actif);
        vue.appartenance().setActive(actif);
        if (!actif) {
            // Désactiver ferme les sessions ouvertes (critère C9).
            sessions.revoquerSessionsDe(userId, vue.appartenance().getClubId());
        }
        publier(actif ? "user.enabled" : "user.disabled", userId, null, null);
    }

    @Transactional
    @PreAuthorize("@perm.a('users.modifier')")
    public void definirRole(UUID userId, Role role) {
        if (!role.attribuableEnR1()) {
            throw new BusinessRuleException("user.role.notAssignableYet");
        }
        Vue vue = vue(userId);
        Role avant = vue.appartenance().getRole();
        if (avant != role) {
            interdireSiDernierAdministrateur(vue);
        }

        vue.appartenance().setRole(role);
        publier("user.role.updated", userId, avant.name(), role.name());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@perm.a('users.consulter')")
    public Droits droits(UUID userId) {
        Vue vue = vue(userId);
        return new Droits(vue.appartenance().getRole(), permissions.effectives(vue.appartenance()),
                permissions.surchargesDe(vue.appartenance()));
    }

    /**
     * Remplace les surcharges d'un utilisateur.
     *
     * <p>Réservé à l'administrateur du compte et non délégable : c'est le seul
     * droit qu'aucune surcharge ne peut accorder (décision 0028).
     */
    @Transactional
    @PreAuthorize("@perm.a('users.permissions.modifier')")
    public Droits definirSurcharges(UUID userId, List<Demande> demandes) {
        Vue vue = vue(userId);
        if (Permissions.detientTout(vue.appartenance().getRole())) {
            throw new BusinessRuleException("user.admin.permissionsNotRestrictable");
        }
        for (Demande demande : demandes) {
            if (!Permissions.estDelegable(demande.code())) {
                throw new BusinessRuleException("security.permission.notDelegable");
            }
        }

        surcharges.deleteAllByMembershipId(vue.appartenance().getId());
        for (Demande demande : demandes) {
            UserPermissionOverride surcharge = new UserPermissionOverride();
            surcharge.setId(UuidV7.next());
            surcharge.setClubId(vue.appartenance().getClubId());
            surcharge.setMembershipId(vue.appartenance().getId());
            surcharge.setPermissionCode(demande.code());
            surcharge.setGranted(demande.granted());
            surcharges.save(surcharge);
        }

        publier("user.permissions.updated", userId, null, String.valueOf(demandes.size()));
        return droitsSansControle(vue);
    }

    @Transactional
    @PreAuthorize("@perm.a('users.sessions.fermer')")
    public void fermerSessions(UUID userId) {
        Vue vue = vue(userId);
        sessions.revoquerSessionsDe(userId, vue.appartenance().getClubId());
    }

    // ------------------------------------------------------------ interne

    /**
     * Un club garde toujours au moins un administrateur actif : sans lui,
     * personne ne peut plus attribuer de droits (critère C12a).
     */
    private void interdireSiDernierAdministrateur(Vue vue) {
        if (vue.appartenance().getRole() != Role.ACCOUNT_ADMIN) {
            return;
        }
        if (appartenances.countByRoleAndActiveTrue(Role.ACCOUNT_ADMIN) <= 1) {
            throw new BusinessRuleException("user.lastAdmin.protected");
        }
    }

    private Droits droitsSansControle(Vue vue) {
        return new Droits(vue.appartenance().getRole(), permissions.effectives(vue.appartenance()),
                permissions.surchargesDe(vue.appartenance()));
    }

    private Vue vue(UUID userId) {
        // L'appartenance est filtrée par club : un utilisateur d'un autre club
        // est donc « introuvable », et non « interdit » (critère C1).
        Membership appartenance = appartenances.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("user.notFound"));
        UserAccount compte = comptes.findById(userId)
                .orElseThrow(() -> new NotFoundException("user.notFound"));
        return new Vue(compte, appartenance);
    }

    private void publier(String action, UUID userId, String avant, String apres) {
        AuthenticatedUser auteur = PermissionChecker.requis();
        evenements.publish(new DomainEvent(auteur.clubId(), action, "UserAccount", userId,
                avant, apres, null));
    }

    private static String resume(UserAccount compte, Role role) {
        return Map.of("email", compte.getEmail(), "role", role.name()).toString();
    }

    /** Un compte et son appartenance au club courant. */
    public record Vue(UserAccount compte, Membership appartenance) {
    }

    /** Droits effectifs d'un utilisateur et surcharges qui s'y appliquent. */
    public record Droits(Role role, java.util.Set<String> effectives,
                         List<UserPermissionOverride> surcharges) {
    }

    /** Demande de surcharge : accorder ou retirer une permission. */
    public record Demande(String code, boolean granted, Object parameter) {
    }
}
