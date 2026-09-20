package ma.clubify.platform.model.dto;

import java.util.List;
import java.util.UUID;

/**
 * L'utilisateur connecté et son club, tels que l'interface les reçoit à la
 * connexion : de quoi afficher l'en-tête, appliquer la marque du club et
 * n'offrir que les actions permises.
 */
public record CurrentUserDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String language,
        String role,
        boolean mfaEnabled,
        MfaStatut mfa,
        List<String> permissions,
        ClubIdentity club) {

    /**
     * Où en est le second facteur, pour que l'écran sache s'il doit inviter à
     * l'activer et jusqu'à quand (décision 0031).
     */
    public record MfaStatut(boolean enabled, boolean expected, boolean blocking,
                            java.time.Instant requiredFrom) {
    }

    /** Ce que l'interface a besoin de savoir du club, et rien de plus. */
    public record ClubIdentity(
            UUID id,
            String name,
            String timezone,
            String currency,
            UUID logoFileId,
            String brandPrimary,
            String brandSecondary,
            int pageSize) {
    }
}
