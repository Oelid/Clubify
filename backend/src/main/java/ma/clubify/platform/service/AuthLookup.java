package ma.clubify.platform.service;

import ma.clubify.platform.model.entity.Membership;
import ma.clubify.platform.model.entity.UserAccount;
import ma.clubify.platform.repository.MembershipRepository;
import ma.clubify.platform.repository.MfaChallengeRepository;
import ma.clubify.platform.repository.RefreshTokenRepository;
import ma.clubify.platform.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Résout un compte et son club <strong>avant</strong> que le contexte de club
 * ne soit posé.
 *
 * <p>C'est le seul moment de l'application où le club est inconnu : c'est
 * précisément ce que l'authentification cherche. Une fois le club résolu, il
 * est posé dans le contexte et tout le reste est filtré normalement (PLT-01).
 */
@Service
public class AuthLookup {

    private final UserAccountRepository comptes;
    private final MembershipRepository appartenances;
    private final MfaChallengeRepository defis;
    private final RefreshTokenRepository sessions;

    public AuthLookup(UserAccountRepository comptes, MembershipRepository appartenances,
                      MfaChallengeRepository defis, RefreshTokenRepository sessions) {
        this.comptes = comptes;
        this.appartenances = appartenances;
        this.defis = defis;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public Optional<Candidat> parEmail(String email) {
        return comptes.findByEmailIgnoreCase(email)
                .flatMap(compte -> appartenances.findForAuthentication(compte.getId())
                        .map(appartenance -> new Candidat(compte, appartenance)));
    }

    /** Club d'un défi de second facteur, avant que le contexte ne soit posé. */
    @Transactional(readOnly = true)
    public Optional<UUID> clubDuDefi(UUID defiId) {
        return defis.clubDuDefi(defiId);
    }

    /** Club d'une session, avant que le contexte ne soit posé. */
    @Transactional(readOnly = true)
    public Optional<UUID> clubDeLaSession(String empreinte) {
        return sessions.clubDeLaSession(empreinte);
    }

    /** Un compte et l'appartenance retenue pour cette connexion. */
    public record Candidat(UserAccount compte, Membership appartenance) {
    }
}
