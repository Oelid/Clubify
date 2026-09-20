package ma.clubify.platform.service;

import ma.clubify.common.event.DomainEvent;
import ma.clubify.common.event.DomainEvents;
import ma.clubify.platform.model.entity.UserAccount;
import ma.clubify.platform.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Compte les tentatives et journalise les échecs (SEC-04, critères C10 et C10b).
 *
 * <p>Ces écritures se font dans leur propre transaction : elles doivent
 * survivre au refus qui suit. Elles ne touchent que {@code user_account} et le
 * journal, tous deux hors du discriminant de club — ce qui permet d'inscrire
 * une tentative sur un identifiant inconnu (décision 0029).
 */
@Service
public class LoginAttemptService {

    private final UserAccountRepository comptes;
    private final ClubSettingService reglages;
    private final DomainEvents evenements;
    private final Clock horloge;

    public LoginAttemptService(UserAccountRepository comptes, ClubSettingService reglages,
                               DomainEvents evenements, Clock horloge) {
        this.comptes = comptes;
        this.reglages = reglages;
        this.evenements = evenements;
        this.horloge = horloge;
    }

    /** Tentative sur un identifiant inconnu : journalisée, sans club. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void identifiantInconnu(String email) {
        journaliser(null, null, email);
    }

    /** Mot de passe erroné : compteur incrémenté, verrou au seuil. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void motDePasseErrone(UUID userId, UUID clubId, String email) {
        UserAccount compte = comptes.findById(userId).orElseThrow();
        int echecs = compte.getFailedAttempts() + 1;
        compte.setFailedAttempts(echecs);
        journaliser(userId, clubId, email);

        if (echecs >= reglages.entier("security.lockout.max_attempts")) {
            long minutes = reglages.entier("security.lockout.minutes");
            compte.setLockedUntil(horloge.instant().plus(Duration.ofMinutes(minutes)));
            evenements.publish(DomainEvent.of(clubId, "auth.account.locked",
                    "UserAccount", userId));
        }
        comptes.save(compte);
    }

    /** Connexion réussie : compteur remis à zéro. */
    @Transactional
    public void reussite(UUID userId, Instant maintenant) {
        comptes.findById(userId).ifPresent(compte -> {
            compte.setFailedAttempts(0);
            compte.setLockedUntil(null);
            compte.setLastLoginAt(maintenant);
        });
    }

    /**
     * Journalise la tentative en écrivant l'identifiant essayé, jamais le mot de
     * passe (critère C10, CLAUDE.md §7).
     */
    private void journaliser(UUID userId, UUID clubId, String email) {
        String details = "{\"email\":\"" + email.replace("\"", "") + "\"}";
        evenements.publish(new DomainEvent(
                clubId, "auth.login.failed", "UserAccount", userId, null, details, null));
    }
}
