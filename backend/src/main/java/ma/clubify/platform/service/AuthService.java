package ma.clubify.platform.service;

import ma.clubify.common.exception.BusinessRuleException;
import ma.clubify.common.security.PermissionChecker;
import ma.clubify.common.security.TenantContext;
import ma.clubify.common.security.TokenService;
import ma.clubify.config.AuthenticatedUser;
import ma.clubify.platform.model.dto.AuthOutcome;
import ma.clubify.platform.model.dto.Tokens;
import ma.clubify.platform.model.entity.Membership;
import ma.clubify.platform.model.entity.UserAccount;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestre la connexion du staff (SEC-01, décisions 0027 et 0029).
 *
 * <p>Elle se fait en deux temps, et ce n'est pas un détail : tant que le club
 * n'est pas connu, aucune donnée cloisonnée ne peut être écrite, puisque le
 * discriminant refuserait un club différent de celui du contexte. On résout donc
 * d'abord le compte et son club ({@link AuthLookup}), on pose le contexte, puis
 * {@link SessionService} écrit dedans.
 *
 * <p>Cette classe n'est volontairement pas transactionnelle : elle délègue à des
 * composants qui le sont, ce qui garantit que le proxy s'applique et donc que
 * l'audit, qui exige une transaction, s'écrive bien.
 */
@Service
public class AuthService {

    private final AuthLookup recherche;
    private final LoginAttemptService tentatives;
    private final SessionService sessions;
    private final PasswordEncoder motsDePasse;
    private final TokenService jetons;
    private final TenantContext contexte;
    private final Clock horloge;

    public AuthService(AuthLookup recherche, LoginAttemptService tentatives,
                       SessionService sessions, PasswordEncoder motsDePasse,
                       TokenService jetons, TenantContext contexte, Clock horloge) {
        this.recherche = recherche;
        this.tentatives = tentatives;
        this.sessions = sessions;
        this.motsDePasse = motsDePasse;
        this.jetons = jetons;
        this.contexte = contexte;
        this.horloge = horloge;
    }

    public AuthOutcome connecter(String email, String motDePasse, String jetonAppareil) {
        Instant maintenant = horloge.instant();
        Optional<AuthLookup.Candidat> trouve = recherche.parEmail(email);

        if (trouve.isEmpty() || !trouve.get().compte().isActive()) {
            tentatives.identifiantInconnu(email);
            throw new BusinessRuleException("auth.credentials.invalid", HttpStatus.UNAUTHORIZED);
        }

        UserAccount compte = trouve.get().compte();
        Membership appartenance = trouve.get().appartenance();
        UUID clubId = appartenance.getClubId();

        if (compte.estVerrouille(maintenant)) {
            throw new BusinessRuleException("auth.account.locked", HttpStatus.LOCKED);
        }

        if (!motsDePasse.matches(motDePasse, compte.getPasswordHash())) {
            tentatives.motDePasseErrone(compte.getId(), clubId, email);
            throw new BusinessRuleException("auth.credentials.invalid", HttpStatus.UNAUTHORIZED);
        }

        // Le club est établi : le contexte est posé, et tout ce qui suit est filtré.
        contexte.set(clubId);
        tentatives.reussite(compte.getId(), maintenant);

        return sessions.poursuivre(compte.getId(), appartenance.getId(), jetonAppareil, maintenant);
    }

    /** Le défi porte son club : le contexte est posé à partir de lui. */
    public Tokens verifierSecondFacteur(UUID defiId, String code, boolean retenirAppareil,
                                        String libelleAppareil) {
        UUID clubId = recherche.clubDuDefi(defiId).orElseThrow(() -> new BusinessRuleException(
                "auth.mfa.challengeInvalid", HttpStatus.UNAUTHORIZED));
        contexte.set(clubId);
        return sessions.verifierSecondFacteur(defiId, code, retenirAppareil, libelleAppareil);
    }

    /** La session porte son club : même principe. */
    public Tokens rafraichir(String jetonOpaque) {
        if (jetonOpaque == null || jetonOpaque.isBlank()) {
            // Ni corps ni cookie : la session n'existe pas, le client doit se connecter.
            throw new BusinessRuleException("auth.refresh.invalid", HttpStatus.UNAUTHORIZED);
        }
        UUID clubId = recherche.clubDeLaSession(jetons.empreinte(jetonOpaque))
                .orElseThrow(() -> new BusinessRuleException(
                        "auth.refresh.invalid", HttpStatus.UNAUTHORIZED));
        contexte.set(clubId);
        return sessions.rafraichir(jetonOpaque);
    }

    public void deconnecter() {
        AuthenticatedUser utilisateur = PermissionChecker.requis();
        sessions.revoquerSessionsDe(utilisateur.userId(), utilisateur.clubId());
    }
}
