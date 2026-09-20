package ma.clubify.platform.service;

import ma.clubify.common.audit.UserActor;
import ma.clubify.common.event.DomainEvent;
import ma.clubify.common.event.DomainEvents;
import ma.clubify.common.exception.BusinessRuleException;
import ma.clubify.common.model.entity.UuidV7;
import ma.clubify.common.security.TokenService;
import ma.clubify.common.security.TotpService;
import ma.clubify.config.AuthenticatedUser;
import ma.clubify.platform.model.dto.AuthOutcome;
import ma.clubify.platform.model.dto.Tokens;
import ma.clubify.platform.model.entity.Membership;
import ma.clubify.platform.model.entity.MfaChallenge;
import ma.clubify.platform.model.entity.RecoveryCode;
import ma.clubify.platform.model.entity.RefreshToken;
import ma.clubify.platform.model.entity.TrustedDevice;
import ma.clubify.platform.model.entity.UserAccount;
import ma.clubify.platform.repository.MfaChallengeRepository;
import ma.clubify.platform.repository.RecoveryCodeRepository;
import ma.clubify.platform.repository.RefreshTokenRepository;
import ma.clubify.platform.repository.TrustedDeviceRepository;
import ma.clubify.platform.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Tout ce qui s'écrit une fois le club connu : jetons, défis, appareils.
 *
 * <p>Séparé d'{@link AuthService} pour une raison précise : ces méthodes sont
 * transactionnelles et l'orchestration ne l'est pas. Les appeler depuis la même
 * classe contournerait le proxy, et l'audit — qui exige une transaction — ne
 * s'écrirait pas.
 */
@Service
public class SessionService {

    private static final Duration DUREE_DEFI = Duration.ofMinutes(5);

    private final UserAccountRepository comptes;
    private final MfaChallengeRepository defis;
    private final RefreshTokenRepository sessions;
    private final TrustedDeviceRepository appareils;
    private final RecoveryCodeRepository codesDeSecours;
    private final PermissionResolver permissions;
    private final ClubSettingService reglages;
    private final PasswordEncoder motsDePasse;
    private final TokenService jetons;
    private final TotpService totp;
    private final DomainEvents evenements;
    private final Clock horloge;

    public SessionService(UserAccountRepository comptes, MfaChallengeRepository defis,
                          RefreshTokenRepository sessions, TrustedDeviceRepository appareils,
                          RecoveryCodeRepository codesDeSecours, PermissionResolver permissions,
                          ClubSettingService reglages, PasswordEncoder motsDePasse,
                          TokenService jetons, TotpService totp, DomainEvents evenements,
                          Clock horloge) {
        this.comptes = comptes;
        this.defis = defis;
        this.sessions = sessions;
        this.appareils = appareils;
        this.codesDeSecours = codesDeSecours;
        this.permissions = permissions;
        this.reglages = reglages;
        this.motsDePasse = motsDePasse;
        this.jetons = jetons;
        this.totp = totp;
        this.evenements = evenements;
        this.horloge = horloge;
    }

    /** Suite de la connexion, une fois le club posé dans le contexte. */
    @Transactional
    public AuthOutcome poursuivre(UUID userId, UUID membershipId, String jetonAppareil,
                                  Instant maintenant) {
        UserAccount compte = comptes.findById(userId).orElseThrow();
        Membership appartenance = permissions.appartenance(membershipId);

        if (compte.isMfaEnabled()) {
            if (appareilDeConfianceValide(compte, jetonAppareil, maintenant)) {
                return authentifier(compte, appartenance, maintenant, null);
            }
            return new AuthOutcome(AuthOutcome.Kind.MFA_REQUIRED, null,
                    creerDefi(compte, appartenance, MfaChallenge.Purpose.LOGIN, maintenant));
        }

        if (appartenance.getRole().exigeSecondFacteur()) {
            // Ce jeton n'ouvre que l'activation du second facteur (critère C6b).
            Tokens provisoires = emettre(compte, appartenance, maintenant, true, null);
            return new AuthOutcome(AuthOutcome.Kind.MFA_ENROLLMENT_REQUIRED, provisoires, null);
        }

        return authentifier(compte, appartenance, maintenant, null);
    }

    @Transactional
    public Tokens verifierSecondFacteur(UUID defiId, String code, boolean retenirAppareil,
                                        String libelleAppareil) {
        Instant maintenant = horloge.instant();
        MfaChallenge defi = defis.findById(defiId)
                .filter(d -> d.estUtilisable(maintenant))
                .orElseThrow(() -> new BusinessRuleException(
                        "auth.mfa.challengeInvalid", HttpStatus.UNAUTHORIZED));

        UserAccount compte = comptes.findById(defi.getUserId()).orElseThrow();
        Membership appartenance = permissions.appartenanceDe(compte.getId());

        if (!codeAccepte(compte, code, maintenant)) {
            throw new BusinessRuleException("auth.mfa.codeInvalid", HttpStatus.UNAUTHORIZED);
        }

        defi.setConsumedAt(maintenant);
        String jetonAppareil = retenirAppareil
                ? marquerAppareilDeConfiance(compte, appartenance, libelleAppareil, maintenant)
                : null;

        return authentifier(compte, appartenance, maintenant, jetonAppareil).tokens();
    }

    /**
     * Ouvre une session pleine après l'activation du second facteur.
     *
     * <p>Le code que l'utilisateur vient de saisir est une preuve de second
     * facteur : lui redemander son mot de passe n'ajouterait rien, et
     * obligerait l'interface à le conserver le temps de l'activation.
     */
    @Transactional
    public Tokens ouvrirApresActivation(UUID userId) {
        UserAccount compte = comptes.findById(userId)
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new BusinessRuleException(
                        "auth.credentials.invalid", HttpStatus.UNAUTHORIZED));
        Membership appartenance = permissions.appartenanceDe(userId);
        return authentifier(compte, appartenance, horloge.instant(), null).tokens();
    }

    @Transactional
    public Tokens rafraichir(String jetonOpaque) {
        Instant maintenant = horloge.instant();
        RefreshToken session = sessions.findByTokenHash(jetons.empreinte(jetonOpaque))
                .filter(s -> s.estUtilisable(maintenant))
                .orElseThrow(() -> new BusinessRuleException(
                        "auth.refresh.invalid", HttpStatus.UNAUTHORIZED));

        UserAccount compte = comptes.findById(session.getUserId())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new BusinessRuleException(
                        "auth.refresh.invalid", HttpStatus.UNAUTHORIZED));
        Membership appartenance = permissions.appartenanceDe(compte.getId());

        // Rotation : l'ancien jeton ne resservira pas.
        session.setRevokedAt(maintenant);

        // Un renouvellement n'est pas une connexion : il ne touche ni la date de
        // dernière connexion, que le gérant lit comme « vu pour la dernière fois »,
        // ni le journal, qu'il noierait sous des entrées sans action humaine.
        // Il évite aussi d'écrire sur le compte, donc d'entrer en conflit avec une
        // connexion simultanée — ce que la recette a mis au jour (scénario S01).
        return emettre(compte, appartenance, maintenant, false, null);
    }

    @Transactional
    public void revoquerSessionsDe(UUID userId, UUID clubId) {
        sessions.revokeAllByUser(userId, horloge.instant());
        evenements.publish(DomainEvent.of(clubId, "auth.logout", "UserAccount", userId));
    }

    // ------------------------------------------------------------ interne

    private AuthOutcome authentifier(UserAccount compte, Membership appartenance,
                                     Instant maintenant, String jetonAppareil) {
        comptes.marquerLaConnexion(compte.getId(), maintenant);
        Tokens emis = emettre(compte, appartenance, maintenant, false, jetonAppareil);
        // Au moment où la connexion réussit, aucun jeton n'est encore posé :
        // l'auteur s'annonce, sinon le journal dirait « système » (SEC-04).
        UserActor.executer(compte.getId(), compte.getEmail(), () ->
                evenements.publish(DomainEvent.of(appartenance.getClubId(),
                        "auth.login.succeeded", "UserAccount", compte.getId())));
        return new AuthOutcome(AuthOutcome.Kind.AUTHENTICATED, emis, null);
    }

    private Tokens emettre(UserAccount compte, Membership appartenance, Instant maintenant,
                           boolean secondFacteurEnAttente, String jetonAppareil) {
        AuthenticatedUser utilisateur = new AuthenticatedUser(
                compte.getId(), compte.getEmail(), appartenance.getClubId(), appartenance.getId(),
                appartenance.getRole(),
                secondFacteurEnAttente ? Set.of() : permissions.effectives(appartenance),
                compte.getLanguage(), secondFacteurEnAttente);

        String acces = jetons.emettre(utilisateur);
        String rafraichissement = jetons.nouveauJetonOpaque();

        RefreshToken session = new RefreshToken();
        session.setId(UuidV7.next());
        session.setClubId(appartenance.getClubId());
        session.setUserId(compte.getId());
        session.setTokenHash(jetons.empreinte(rafraichissement));
        session.setExpiresAt(maintenant.plus(jetons.dureeRafraichissement()));
        sessions.save(session);

        return new Tokens(acces, rafraichissement, jetons.dureeAcces().toSeconds(), jetonAppareil);
    }

    private UUID creerDefi(UserAccount compte, Membership appartenance,
                           MfaChallenge.Purpose objet, Instant maintenant) {
        MfaChallenge defi = new MfaChallenge();
        defi.setId(UuidV7.next());
        defi.setClubId(appartenance.getClubId());
        defi.setUserId(compte.getId());
        defi.setPurpose(objet);
        defi.setExpiresAt(maintenant.plus(DUREE_DEFI));
        return defis.save(defi).getId();
    }

    /** Un code TOTP, ou un code de secours à usage unique (benchmark B5). */
    private boolean codeAccepte(UserAccount compte, String code, Instant maintenant) {
        if (compte.getMfaSecret() != null && totp.verifier(compte.getMfaSecret(), code)) {
            return true;
        }
        for (RecoveryCode secours : codesDeSecours.findAllByUserIdAndUsedAtIsNull(compte.getId())) {
            if (motsDePasse.matches(code, secours.getCodeHash())) {
                secours.setUsedAt(maintenant);
                return true;
            }
        }
        return false;
    }

    private boolean appareilDeConfianceValide(UserAccount compte, String jetonAppareil,
                                              Instant maintenant) {
        if (jetonAppareil == null || jetonAppareil.isBlank()) {
            return false;
        }
        return appareils.findByTokenHash(jetons.empreinte(jetonAppareil))
                .filter(appareil -> appareil.getUserId().equals(compte.getId()))
                .filter(appareil -> appareil.estUtilisable(maintenant))
                .map(appareil -> {
                    appareil.setLastUsedAt(maintenant);
                    return true;
                })
                .orElse(false);
    }

    private String marquerAppareilDeConfiance(UserAccount compte, Membership appartenance,
                                              String libelle, Instant maintenant) {
        String jetonAppareil = jetons.nouveauJetonOpaque();
        long jours = reglages.entier("security.mfa.trusted_device_days");

        TrustedDevice appareil = new TrustedDevice();
        appareil.setId(UuidV7.next());
        appareil.setClubId(appartenance.getClubId());
        appareil.setUserId(compte.getId());
        appareil.setTokenHash(jetons.empreinte(jetonAppareil));
        appareil.setLabel(libelle == null || libelle.isBlank() ? "Appareil" : libelle);
        appareil.setExpiresAt(maintenant.plus(Duration.ofDays(jours)));
        appareils.save(appareil);

        evenements.publish(DomainEvent.of(appartenance.getClubId(), "auth.device.trusted",
                "TrustedDevice", appareil.getId()));
        return jetonAppareil;
    }
}
