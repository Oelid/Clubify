package ma.clubify.platform.controller;

import ma.clubify.generated.api.UsersApi;
import ma.clubify.generated.model.PageMeta;
import ma.clubify.generated.model.PermissionDefinition;
import ma.clubify.generated.model.PermissionOverride;
import ma.clubify.generated.model.Role;
import ma.clubify.generated.model.SetUserRoleRequest;
import ma.clubify.generated.model.SetUserStatusRequest;
import ma.clubify.generated.model.User;
import ma.clubify.generated.model.UserCreateRequest;
import ma.clubify.generated.model.UserPage;
import ma.clubify.generated.model.UserPermissions;
import ma.clubify.generated.model.UserUpdateRequest;
import ma.clubify.common.security.Permissions;
import ma.clubify.platform.model.dto.PermissionsDto;
import ma.clubify.platform.model.dto.UserDto;
import ma.clubify.platform.service.MfaService;
import ma.clubify.platform.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utilisateurs, rôles et permissions.
 *
 * <p>Les contrôles d'accès vivent dans {@link UserService} : ces annotations
 * déclarent l'intention et satisfont la règle « aucun point d'entrée sans
 * permission déclarée », que le test d'architecture vérifie (critère C11b).
 */
@RestController
public class UsersController implements UsersApi {

    private final UserService utilisateurs;
    private final MfaService secondFacteur;

    public UsersController(UserService utilisateurs, MfaService secondFacteur) {
        this.utilisateurs = utilisateurs;
        this.secondFacteur = secondFacteur;
    }

    @Override
    @PreAuthorize("@perm.a('users.consulter')")
    public ResponseEntity<UserPage> listUsers(Integer page, Integer size, Role role, Boolean active) {
        Page<UserDto> trouvees = utilisateurs.lister(
                PageRequest.of(page == null ? 0 : page, size == null ? 20 : size));

        UserPage reponse = new UserPage();
        reponse.setContent(trouvees.getContent().stream().map(UsersController::versContrat).toList());

        PageMeta meta = new PageMeta();
        meta.setPage(trouvees.getNumber());
        meta.setSize(trouvees.getSize());
        meta.setTotalElements(trouvees.getTotalElements());
        meta.setTotalPages(trouvees.getTotalPages());
        reponse.setPage(meta);
        return ResponseEntity.ok(reponse);
    }

    @Override
    @PreAuthorize("@perm.a('users.creer')")
    public ResponseEntity<User> createUser(UserCreateRequest demande) {
        UserDto creee = utilisateurs.creer(
                demande.getEmail(), demande.getFirstName(), demande.getLastName(),
                demande.getPhone(), demande.getLanguage(),
                ma.clubify.platform.model.Role.valueOf(demande.getRole().getValue()),
                demande.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(versContrat(creee));
    }

    @Override
    @PreAuthorize("@perm.a('users.consulter')")
    public ResponseEntity<User> getUser(UUID userId) {
        return ResponseEntity.ok(versContrat(utilisateurs.lire(userId)));
    }

    @Override
    @PreAuthorize("@perm.a('users.modifier')")
    public ResponseEntity<User> updateUser(UUID userId, UserUpdateRequest demande) {
        return ResponseEntity.ok(versContrat(utilisateurs.modifier(
                userId, demande.getFirstName(), demande.getLastName(),
                demande.getPhone(), demande.getLanguage())));
    }

    @Override
    @PreAuthorize("@perm.a('users.desactiver')")
    public ResponseEntity<Void> setUserStatus(UUID userId, SetUserStatusRequest demande) {
        utilisateurs.definirStatut(userId, demande.getActive());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@perm.a('users.modifier')")
    public ResponseEntity<Void> setUserRole(UUID userId, SetUserRoleRequest demande) {
        utilisateurs.definirRole(userId,
                ma.clubify.platform.model.Role.valueOf(demande.getRole().getValue()));
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@perm.a('users.consulter')")
    public ResponseEntity<UserPermissions> getUserPermissions(UUID userId) {
        return ResponseEntity.ok(versContrat(utilisateurs.droits(userId)));
    }

    @Override
    @PreAuthorize("@perm.a('users.permissions.modifier')")
    public ResponseEntity<UserPermissions> setUserPermissionOverrides(
            UUID userId, List<PermissionOverride> demandes) {
        List<UserService.Demande> souhaits = demandes.stream()
                .map(d -> new UserService.Demande(d.getCode(), d.getGranted(), d.getParameter()))
                .toList();
        return ResponseEntity.ok(versContrat(utilisateurs.definirSurcharges(userId, souhaits)));
    }

    @Override
    @PreAuthorize("@perm.a('users.sessions.fermer')")
    public ResponseEntity<Void> revokeUserSessions(UUID userId) {
        utilisateurs.fermerSessions(userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@perm.a('users.mfa.reinitialiser')")
    public ResponseEntity<Void> resetUserMfa(UUID userId) {
        secondFacteur.reinitialiserPour(userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@perm.authentifie()")
    public ResponseEntity<List<PermissionDefinition>> listPermissions() {
        List<PermissionDefinition> catalogue = Permissions.CATALOGUE.stream()
                .map(code -> {
                    PermissionDefinition definition = new PermissionDefinition();
                    definition.setCode(code);
                    definition.setParameterized(false);
                    definition.setDefaultRoles(rolesParDefaut(code));
                    return definition;
                })
                .toList();
        return ResponseEntity.ok(catalogue);
    }

    // ------------------------------------------------------- conversions

    private static List<Role> rolesParDefaut(String permission) {
        List<Role> roles = new ArrayList<>();
        for (ma.clubify.platform.model.Role role
                : ma.clubify.platform.model.Role.values()) {
            if (Permissions.parDefaut(role).contains(permission)) {
                roles.add(Role.fromValue(role.name()));
            }
        }
        return roles;
    }

    private static User versContrat(UserDto vue) {
        User utilisateur = new User();
        utilisateur.setId(vue.id());
        utilisateur.setEmail(vue.email());
        utilisateur.setFirstName(vue.firstName());
        utilisateur.setLastName(vue.lastName());
        utilisateur.setPhone(vue.phone());
        utilisateur.setLanguage(vue.language());
        utilisateur.setRole(Role.fromValue(vue.role()));
        utilisateur.setActive(vue.active());
        utilisateur.setMfaEnabled(vue.mfaEnabled());
        if (vue.lastLoginAt() != null) {
            utilisateur.setLastLoginAt(vue.lastLoginAt().atOffset(ZoneOffset.UTC));
        }
        return utilisateur;
    }

    private static UserPermissions versContrat(PermissionsDto droits) {
        UserPermissions reponse = new UserPermissions();
        reponse.setRole(Role.fromValue(droits.role()));
        reponse.setEffective(new ArrayList<>(droits.effective()));
        reponse.setOverrides(droits.overrides().stream().map(surcharge -> {
            PermissionOverride contrat = new PermissionOverride();
            contrat.setCode(surcharge.code());
            contrat.setGranted(surcharge.granted());
            return contrat;
        }).toList());
        return reponse;
    }
}
