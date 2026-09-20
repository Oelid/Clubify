package ma.clubify.config;

import ma.clubify.platform.model.Role;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Utilisateur porté par le jeton : qui il est, dans quel club, avec quel rôle et
 * quelles permissions effectives.
 *
 * @param mfaPending vrai tant que le second facteur n'est pas franchi : le jeton
 *                   n'ouvre alors que son activation ou sa vérification (C6b)
 * @param email      adresse du compte, reportée telle quelle dans le journal
 *                   d'audit : une trace doit rester lisible après un renommage
 * @param issuedAt   instant d'émission, comparé à la borne de validité du compte
 *                   pour permettre une révocation immédiate (C8b, C9)
 */
public record AuthenticatedUser(
        UUID userId,
        String email,
        UUID clubId,
        UUID membershipId,
        Role role,
        Set<String> permissions,
        String language,
        boolean mfaPending,
        Instant issuedAt) {

    public AuthenticatedUser(UUID userId, String email, UUID clubId, UUID membershipId,
                             Role role, Set<String> permissions, String language,
                             boolean mfaPending) {
        this(userId, email, clubId, membershipId, role, permissions, language, mfaPending, null);
    }

    public boolean detient(String permission) {
        return permissions.contains(permission);
    }
}
