package ma.clubify.config;

import ma.clubify.platform.model.entity.Role;

import java.util.Set;
import java.util.UUID;

/**
 * Utilisateur porté par le jeton : qui il est, dans quel club, avec quel rôle et
 * quelles permissions effectives.
 *
 * @param mfaPending vrai tant que le second facteur n'est pas franchi : le jeton
 *                   n'ouvre alors que son activation ou sa vérification (C6b).
 */
public record AuthenticatedUser(
        UUID userId,
        UUID clubId,
        UUID membershipId,
        Role role,
        Set<String> permissions,
        String language,
        boolean mfaPending) {

    public boolean detient(String permission) {
        return permissions.contains(permission);
    }
}
