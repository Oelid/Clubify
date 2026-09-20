package ma.clubify.platform.service;

import jakarta.persistence.criteria.Predicate;
import ma.clubify.common.model.entity.AuditLog;
import ma.clubify.common.repository.AuditLogRepository;
import ma.clubify.common.security.PermissionChecker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Recherche dans le journal (SEC-04).
 *
 * <p>{@code AuditLog} ne porte pas le discriminant de club — les événements
 * d'authentification n'en ont pas — le filtre par club est donc explicite ici.
 */
@Service
public class AuditQueryService {

    private final AuditLogRepository journal;

    public AuditQueryService(AuditLogRepository journal) {
        this.journal = journal;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@perm.a('audit.consulter')")
    public Page<AuditLog> rechercher(UUID auteurId, String action, String typeEntite,
                                     UUID entiteId, Instant depuis, Instant jusqua,
                                     Pageable pagination) {
        UUID clubId = PermissionChecker.requis().clubId();

        Specification<AuditLog> criteres = (racine, requete, constructeur) -> {
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(constructeur.equal(racine.get("clubId"), clubId));

            if (auteurId != null) {
                conditions.add(constructeur.equal(racine.get("actorId"), auteurId));
            }
            if (action != null && !action.isBlank()) {
                conditions.add(constructeur.equal(racine.get("action"), action));
            }
            if (typeEntite != null && !typeEntite.isBlank()) {
                conditions.add(constructeur.equal(racine.get("entityType"), typeEntite));
            }
            if (entiteId != null) {
                conditions.add(constructeur.equal(racine.get("entityId"), entiteId));
            }
            if (depuis != null) {
                conditions.add(constructeur.greaterThanOrEqualTo(racine.get("occurredAt"), depuis));
            }
            if (jusqua != null) {
                conditions.add(constructeur.lessThanOrEqualTo(racine.get("occurredAt"), jusqua));
            }
            return constructeur.and(conditions.toArray(new Predicate[0]));
        };

        Pageable triee = Pageable.unpaged().equals(pagination)
                ? pagination
                : org.springframework.data.domain.PageRequest.of(
                        pagination.getPageNumber(), pagination.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "occurredAt"));
        return journal.findAll(criteres, triee);
    }
}
