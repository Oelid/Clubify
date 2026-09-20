package ma.clubify.common.event;

import ma.clubify.common.model.entity.OutboxEvent;
import ma.clubify.common.model.entity.UuidV7;
import ma.clubify.common.repository.OutboxEventRepository;
import ma.clubify.common.util.Json;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Dépose un effet externe dans l'outbox, dans la transaction de l'action.
 *
 * <p>Si l'action échoue, rien n'est déposé : l'effet n'existe que si le fait
 * métier a eu lieu (critère C20).
 */
@Component
public class OutboxPublisher {

    private final OutboxEventRepository outbox;
    private final Json json;
    private final Clock horloge;

    public OutboxPublisher(OutboxEventRepository outbox, Json json, Clock horloge) {
        this.outbox = outbox;
        this.json = json;
        this.horloge = horloge;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deposer(ExternalEffect effet) {
        OutboxEvent entree = new OutboxEvent();
        entree.setId(UuidV7.next());
        entree.setClubId(effet.clubId());
        entree.setEventType(effet.type());
        entree.setPayload(json.de(effet.donnees()));
        entree.setAvailableAt(horloge.instant());
        outbox.save(entree);
    }
}
