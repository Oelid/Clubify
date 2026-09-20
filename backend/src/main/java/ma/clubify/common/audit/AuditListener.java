package ma.clubify.common.audit;

import ma.clubify.common.event.DomainEvent;
import ma.clubify.common.model.entity.AuditLog;
import ma.clubify.common.model.entity.UuidV7;
import ma.clubify.common.repository.AuditLogRepository;
import ma.clubify.config.AuthenticatedUser;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.Clock;

/**
 * Écrit le journal d'audit depuis les événements métier (SEC-04).
 *
 * <p>Volontairement <strong>synchrone et bloquant</strong>, dans la transaction
 * de l'action : si le journal échoue, l'action échoue. Une opération sur
 * l'argent ou sur des données d'enfants sans trace ne doit pas exister
 * (décision 0029, critère C22b). C'est l'unique exception à la règle « un
 * abonné en échec ne bloque pas le métier » ; tout autre abonné passe par
 * l'outbox.
 */
@Component
public class AuditListener {

    private final AuditLogRepository journal;
    private final Clock horloge;
    private final AuditFailureSwitch interrupteur;

    public AuditListener(AuditLogRepository journal, Clock horloge,
                         AuditFailureSwitch interrupteur) {
        this.journal = journal;
        this.horloge = horloge;
        this.interrupteur = interrupteur;
    }

    @Order(0)
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onDomainEvent(DomainEvent evenement) {
        interrupteur.echouerSiDemande();

        AuditLog entree = new AuditLog();
        entree.setId(UuidV7.next());
        entree.setClubId(evenement.clubId());
        entree.setOccurredAt(horloge.instant());
        entree.setAction(evenement.action());
        entree.setEntityType(evenement.entityType());
        entree.setEntityId(evenement.entityId());
        entree.setBeforeState(evenement.before());
        entree.setAfterState(evenement.after());
        entree.setReason(evenement.reason());
        entree.setRequestId(identifiantDeDemande());
        renseignerAuteur(entree);

        journal.save(entree);
    }

    private void renseignerAuteur(AuditLog entree) {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        if (authentification != null
                && authentification.getPrincipal() instanceof AuthenticatedUser utilisateur) {
            entree.setActorType(AuditLog.ActorType.USER);
            entree.setActorId(utilisateur.userId());
            // Le libellé est figé à l'instant de l'action : un compte renommé ne
            // doit pas réécrire l'histoire.
            entree.setActorLabel(utilisateur.email());
            return;
        }

        UserActor.Auteur auteur = UserActor.courant();
        if (auteur != null) {
            entree.setActorType(AuditLog.ActorType.USER);
            entree.setActorId(auteur.userId());
            entree.setActorLabel(auteur.libelle());
            return;
        }
        // Hors de toute demande authentifiée : c'est le système qui agit, et la
        // règle se nomme, sans quoi le journal dirait « système » sans dire quoi.
        entree.setActorType(AuditLog.ActorType.SYSTEM);
        entree.setActorLabel(SystemActor.regleCourante());
    }

    private String identifiantDeDemande() {
        var attributs = RequestContextHolder.getRequestAttributes();
        return attributs == null ? null : attributs.getSessionId();
    }
}
