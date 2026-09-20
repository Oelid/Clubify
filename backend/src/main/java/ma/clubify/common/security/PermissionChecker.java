package ma.clubify.common.security;

import ma.clubify.config.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Vérifie une permission depuis une annotation {@code @PreAuthorize}, posée sur
 * les services et non seulement sur les contrôleurs (SEC-02, critère C11).
 *
 * <p>Nommé {@code perm} pour que les annotations restent lisibles :
 * {@code @PreAuthorize("@perm.a('users.consulter')")}.
 */
@Component("perm")
public class PermissionChecker {

    /** L'utilisateur courant détient-il cette permission ? */
    public boolean a(String permission) {
        AuthenticatedUser utilisateur = courant();
        if (utilisateur == null) {
            return false;
        }
        // Tant que le second facteur n'est pas franchi, le jeton n'ouvre rien
        // d'autre que son activation (critère C6b).
        if (utilisateur.mfaPending()) {
            return false;
        }
        return utilisateur.detient(permission);
    }

    /** Vrai dès que l'une des permissions est détenue. */
    public boolean une(String... permissions) {
        for (String permission : permissions) {
            if (a(permission)) {
                return true;
            }
        }
        return false;
    }

    /** Authentifié, second facteur éventuellement encore à activer. */
    public boolean authentifie() {
        return courant() != null;
    }

    public static AuthenticatedUser courant() {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        return authentification != null
                && authentification.getPrincipal() instanceof AuthenticatedUser utilisateur
                ? utilisateur
                : null;
    }

    /** L'utilisateur courant, ou une erreur : appelé depuis un point protégé. */
    public static AuthenticatedUser requis() {
        AuthenticatedUser utilisateur = courant();
        if (utilisateur == null) {
            throw new IllegalStateException("Aucun utilisateur authentifié dans le contexte.");
        }
        return utilisateur;
    }
}
