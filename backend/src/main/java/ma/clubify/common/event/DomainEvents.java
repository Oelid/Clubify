package ma.clubify.common.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Point de publication unique des faits métier (6.3). Un service ne notifie
 * jamais directement un autre domaine : il publie, et les abonnés réagissent.
 */
@Component
public class DomainEvents {

    private final ApplicationEventPublisher publisher;

    public DomainEvents(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(DomainEvent evenement) {
        publisher.publishEvent(evenement);
    }
}
