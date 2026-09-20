package ma.clubify.platform.service;

import ma.clubify.common.event.DomainEvent;
import ma.clubify.common.event.DomainEvents;
import ma.clubify.common.exception.BusinessRuleException;
import ma.clubify.common.exception.NotFoundException;
import ma.clubify.common.model.entity.UuidV7;
import ma.clubify.common.security.PermissionChecker;
import ma.clubify.common.security.TotpService;
import ma.clubify.config.AuthenticatedUser;
import ma.clubify.platform.model.entity.RecoveryCode;
import ma.clubify.platform.model.dto.Tokens;
import ma.clubify.platform.model.dto.TrustedDeviceDto;
import ma.clubify.platform.model.entity.TrustedDevice;
import ma.clubify.platform.model.entity.UserAccount;
import ma.clubify.platform.repository.RecoveryCodeRepository;
import ma.clubify.platform.repository.TrustedDeviceRepository;
import ma.clubify.platform.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Activation, réinitialisation et codes de secours du second facteur. */
@Service
public class MfaService {

    private static final int NOMBRE_DE_CODES = 8;
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final SecureRandom alea = new SecureRandom();
    private final UserAccountRepository comptes;
    private final RecoveryCodeRepository codes;
    private final TrustedDeviceRepository appareils;
    private final TotpService totp;
    private final PasswordEncoder encodeur;
    private final DomainEvents evenements;
    private final SessionService sessions;
    private final Clock horloge;

    public MfaService(UserAccountRepository comptes, RecoveryCodeRepository codes,
                      TrustedDeviceRepository appareils, TotpService totp,
                      PasswordEncoder encodeur, DomainEvents evenements,
                      SessionService sessions, Clock horloge) {
        this.comptes = comptes;
        this.codes = codes;
        this.appareils = appareils;
        this.totp = totp;
        this.encodeur = encodeur;
        this.evenements = evenements;
        this.sessions = sessions;
        this.horloge = horloge;
    }

    /** Prépare l'activation : secret à scanner et codes de secours à conserver. */
    @Transactional
    public Preparation preparer() {
        AuthenticatedUser authentifie = PermissionChecker.requis();
        UserAccount compte = compte(authentifie.userId());

        String secret = totp.nouveauSecret();
        compte.setMfaSecret(secret);

        List<String> enClair = regenererCodes(authentifie, compte);
        return new Preparation(totp.otpauthUri(secret, compte.getEmail(), "Clubify"), enClair);
    }

    /** Confirme l'activation par un premier code : sans cela, rien n'est activé. */
    @Transactional
    public Tokens confirmer(String code) {
        AuthenticatedUser authentifie = PermissionChecker.requis();
        UserAccount compte = compte(authentifie.userId());

        if (compte.getMfaSecret() == null || !totp.verifier(compte.getMfaSecret(), code)) {
            throw new BusinessRuleException("auth.mfa.codeInvalid");
        }
        compte.setMfaEnabled(true);
        evenements.publish(DomainEvent.of(authentifie.clubId(), "auth.mfa.enabled",
                "UserAccount", compte.getId()));

        // Le code saisi vaut second facteur : la session s'ouvre pleinement.
        return sessions.ouvrirApresActivation(compte.getId());
    }

    @Transactional
    public List<String> regenererCodesDeSecours() {
        AuthenticatedUser authentifie = PermissionChecker.requis();
        return regenererCodes(authentifie, compte(authentifie.userId()));
    }

    /** Réinitialise le second facteur d'un autre utilisateur (critère C6c). */
    @Transactional
    public void reinitialiserPour(UUID userId) {
        AuthenticatedUser authentifie = PermissionChecker.requis();
        UserAccount compte = compte(userId);

        compte.setMfaEnabled(false);
        compte.setMfaSecret(null);
        codes.deleteAllByUserId(userId);
        appareils.findAllByUserIdAndRevokedAtIsNull(userId)
                .forEach(appareil -> appareil.setRevokedAt(horloge.instant()));

        evenements.publish(DomainEvent.of(authentifie.clubId(), "auth.mfa.reset",
                "UserAccount", userId));
    }

    @Transactional(readOnly = true)
    public List<TrustedDeviceDto> appareilsDeConfiance() {
        return appareils.findAllByUserIdAndRevokedAtIsNull(PermissionChecker.requis().userId())
                .stream()
                .map(appareil -> new TrustedDeviceDto(appareil.getId(), appareil.getLabel(),
                        appareil.getLastUsedAt(), appareil.getExpiresAt()))
                .toList();
    }

    @Transactional
    public void revoquerAppareil(UUID appareilId) {
        AuthenticatedUser authentifie = PermissionChecker.requis();
        TrustedDevice appareil = appareils.findById(appareilId)
                .filter(a -> a.getUserId().equals(authentifie.userId()))
                .orElseThrow(() -> new NotFoundException("device.notFound"));

        appareil.setRevokedAt(horloge.instant());
        evenements.publish(DomainEvent.of(authentifie.clubId(), "auth.device.revoked",
                "TrustedDevice", appareilId));
    }

    private List<String> regenererCodes(AuthenticatedUser authentifie, UserAccount compte) {
        codes.deleteAllByUserId(compte.getId());

        List<String> enClair = new ArrayList<>();
        for (int i = 0; i < NOMBRE_DE_CODES; i++) {
            String code = genererCode();
            enClair.add(code);

            RecoveryCode secours = new RecoveryCode();
            secours.setId(UuidV7.next());
            secours.setClubId(authentifie.clubId());
            secours.setUserId(compte.getId());
            // Seule l'empreinte est conservée : le code ne se retrouve pas.
            secours.setCodeHash(encodeur.encode(code));
            codes.save(secours);
        }
        return enClair;
    }

    private String genererCode() {
        StringBuilder code = new StringBuilder(9);
        for (int i = 0; i < 9; i++) {
            code.append(i == 4 ? '-' : ALPHABET.charAt(alea.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }

    private UserAccount compte(UUID userId) {
        return comptes.findById(userId).orElseThrow(() -> new NotFoundException("user.notFound"));
    }

    /** Ce qu'il faut montrer une seule fois à l'utilisateur qui active. */
    public record Preparation(String otpauthUri, List<String> recoveryCodes) {
    }
}
