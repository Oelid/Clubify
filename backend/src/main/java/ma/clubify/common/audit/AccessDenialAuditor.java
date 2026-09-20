package ma.clubify.common.audit;

import ma.clubify.common.event.DomainEvent;
import ma.clubify.common.event.DomainEvents;
import ma.clubify.common.security.PermissionChecker;
import ma.clubify.config.AuthenticatedUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inscrit les refus d'accès au journal (SEC-04, critère C11).
 *
 * <p>Dans sa propre transaction : l'action refusée n'en a pas, et la trace doit
 * survivre au refus. Savoir qui a tenté quoi vaut autant que d'avoir refusé.
 */
@Component
public class AccessDenialAuditor {

    private final DomainEvents evenements;

    public AccessDenialAuditor(DomainEvents evenements) {
        this.evenements = evenements;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refus(String chemin) {
        AuthenticatedUser utilisateur = PermissionChecker.courant();
        if (utilisateur == null) {
            // Non authentifié : le refus se voit déjà dans les tentatives de connexion.
            return;
        }
        evenements.publish(new DomainEvent(utilisateur.clubId(), "security.access.denied",
                "Endpoint", null, null, null, chemin));
    }
}
