package ma.clubify.platform.model.dto;

/**
 * Couple de jetons délivré à l'issue d'une authentification.
 *
 * @param deviceToken jeton d'appareil de confiance, présent seulement lorsque
 *                    l'utilisateur vient de demander à être reconnu (benchmark B4)
 */
public record Tokens(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        String deviceToken) {
}
