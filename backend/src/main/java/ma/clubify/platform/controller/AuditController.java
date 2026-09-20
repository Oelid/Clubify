package ma.clubify.platform.controller;

import ma.clubify.common.model.entity.AuditLog;
import ma.clubify.generated.api.AuditApi;
import ma.clubify.generated.model.AuditEntry;
import ma.clubify.generated.model.AuditEntryPage;
import ma.clubify.generated.model.PageMeta;
import ma.clubify.platform.service.AuditQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Consultation du journal d'audit.
 *
 * <p>Avec ses filtres : un journal sans recherche ne sert pas au gérant qui
 * cherche l'origine d'un écart de caisse (benchmark B3, critère C19b).
 */
@RestController
public class AuditController implements AuditApi {

    private final AuditQueryService journal;

    public AuditController(AuditQueryService journal) {
        this.journal = journal;
    }

    @Override
    @PreAuthorize("@perm.a('audit.consulter')")
    public ResponseEntity<AuditEntryPage> searchAuditEntries(
            Integer page, Integer size, UUID actorId, String action, String entityType,
            UUID entityId, OffsetDateTime from, OffsetDateTime to) {

        Page<AuditLog> trouvees = journal.rechercher(
                actorId, action, entityType, entityId,
                from == null ? null : from.toInstant(),
                to == null ? null : to.toInstant(),
                PageRequest.of(page == null ? 0 : page, size == null ? 20 : size));

        AuditEntryPage reponse = new AuditEntryPage();
        reponse.setContent(trouvees.getContent().stream().map(AuditController::versContrat).toList());

        PageMeta meta = new PageMeta();
        meta.setPage(trouvees.getNumber());
        meta.setSize(trouvees.getSize());
        meta.setTotalElements(trouvees.getTotalElements());
        meta.setTotalPages(trouvees.getTotalPages());
        reponse.setPage(meta);
        return ResponseEntity.ok(reponse);
    }

    private static AuditEntry versContrat(AuditLog entree) {
        AuditEntry contrat = new AuditEntry();
        contrat.setId(entree.getId());
        contrat.setOccurredAt(entree.getOccurredAt().atOffset(ZoneOffset.UTC));
        contrat.setActorType(AuditEntry.ActorTypeEnum.fromValue(entree.getActorType().name()));
        contrat.setActorId(entree.getActorId());
        contrat.setActorLabel(entree.getActorLabel());
        contrat.setAction(entree.getAction());
        contrat.setEntityType(entree.getEntityType());
        contrat.setEntityId(entree.getEntityId());
        contrat.setBefore(entree.getBeforeState());
        contrat.setAfter(entree.getAfterState());
        contrat.setReason(entree.getReason());
        contrat.setRequestId(entree.getRequestId());
        return contrat;
    }
}
