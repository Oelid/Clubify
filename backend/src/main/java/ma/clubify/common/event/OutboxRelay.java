package ma.clubify.common.event;

import ma.clubify.common.model.entity.OutboxEvent;
import ma.clubify.common.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;

/**
 * Produit les effets déposés dans l'outbox.
 *
 * <p>Chaque effet est marqué traité dès qu'il aboutit : un rejeu ne le produit
 * donc pas deux fois (critère C21). Un échec est consigné et laisse l'effet en
 * attente, sans jamais remonter à l'action métier, déjà terminée (critère C22).
 */
@Component
public class OutboxRelay {

    private static final Logger JOURNAL = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int TAILLE_DE_LOT = 50;

    private final OutboxEventRepository outbox;
    private final List<ExternalEffectHandler> abonnes;
    private final Clock horloge;

    public OutboxRelay(OutboxEventRepository outbox, List<ExternalEffectHandler> abonnes,
                       Clock horloge) {
        this.outbox = outbox;
        this.abonnes = abonnes;
        this.horloge = horloge;
    }

    /** Traite un lot d'effets et retourne le nombre d'effets aboutis. */
    @Transactional
    public int traiterUnLot() {
        List<OutboxEvent> aTraiter = outbox.aTraiter(horloge.instant(), TAILLE_DE_LOT);
        int aboutis = 0;

        for (OutboxEvent entree : aTraiter) {
            try {
                produire(entree);
                entree.setProcessedAt(horloge.instant());
                entree.setLastError(null);
                aboutis++;
            } catch (RuntimeException echec) {
                entree.setAttempts(entree.getAttempts() + 1);
                // Le message d'erreur ne contient aucune donnée personnelle.
                entree.setLastError(echec.getClass().getSimpleName());
                JOURNAL.warn("Effet externe {} en échec, tentative {}",
                        entree.getEventType(), entree.getAttempts());
            }
            outbox.save(entree);
        }
        return aboutis;
    }

    private void produire(OutboxEvent entree) {
        ExternalEffect effet = new ExternalEffect(
                entree.getClubId(), entree.getEventType(), Map.of());
        for (ExternalEffectHandler abonne : abonnes) {
            if (abonne.accepte(entree.getEventType())) {
                abonne.produire(effet);
            }
        }
    }
}
