package ma.clubify.platform.service;

import ma.clubify.common.audit.SystemActor;
import ma.clubify.common.event.DomainEvent;
import ma.clubify.common.event.DomainEvents;
import ma.clubify.platform.model.entity.MfaChallenge;
import ma.clubify.platform.repository.MfaChallengeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Ferme les défis de second facteur expirés.
 *
 * <p>Première règle automatique de l'application : elle s'inscrit au journal
 * sous l'auteur « système », en se nommant, et non sous l'utilisateur dont le
 * défi a expiré (critère C16b).
 */
@Service
public class MfaChallengeCleanup {

    public static final String NOM = "auth.challenge.expire";

    private final MfaChallengeRepository defis;
    private final DomainEvents evenements;
    private final Clock horloge;

    public MfaChallengeCleanup(MfaChallengeRepository defis, DomainEvents evenements,
                               Clock horloge) {
        this.defis = defis;
        this.evenements = evenements;
        this.horloge = horloge;
    }

    /** Toutes les cinq minutes : un défi ne vit que cinq minutes. */
    @Scheduled(fixedDelay = 300_000)
    public void executer() {
        SystemActor.executer(NOM, this::fermerLesExpires);
    }

    @Transactional
    public void fermerLesExpires() {
        List<MfaChallenge> expires = defis.findExpirees(horloge.instant());
        if (expires.isEmpty()) {
            return;
        }
        expires.forEach(defi -> defi.setConsumedAt(horloge.instant()));
        evenements.publish(new DomainEvent(
                expires.getFirst().getClubId(), NOM, "MfaChallenge", null,
                null, null, null));
    }
}
