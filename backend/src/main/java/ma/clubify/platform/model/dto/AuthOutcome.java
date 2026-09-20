package ma.clubify.platform.model.dto;

import java.util.UUID;

/**
 * Issue d'une tentative de connexion. Trois cas seulement, et le contrat les
 * nomme : le client n'a jamais à les deviner.
 */
public record AuthOutcome(Kind kind, Tokens tokens, UUID mfaChallengeId) {

    public enum Kind {
        /** Jetons délivrés, l'utilisateur peut travailler. */
        AUTHENTICATED,
        /** Second facteur actif : un code est attendu. */
        MFA_REQUIRED,
        /** Rôle l'exigeant, mais second facteur pas encore activé (C6b). */
        MFA_ENROLLMENT_REQUIRED
    }
}
