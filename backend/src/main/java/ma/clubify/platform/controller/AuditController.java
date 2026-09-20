package ma.clubify.platform.controller;

import ma.clubify.generated.api.AuditApi;
import ma.clubify.generated.model.AuditEntry;
import ma.clubify.generated.model.AuditEntryPage;
import ma.clubify.generated.model.PageMeta;
import ma.clubify.platform.model.dto.AuditEntryDto;
import ma.clubify.platform.service.AuditQueryService;
import ma.clubify.platform.service.PaginationPolicy;
import org.springframework.data.domain.Page;
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
    private final PaginationPolicy pagination;

    public AuditController(AuditQueryService journal, PaginationPolicy pagination) {
        this.journal = journal;
        this.pagination = pagination;
    }

    @Override
    @PreAuthorize("@perm.a('audit.consulter')")
    public ResponseEntity<AuditEntryPage> searchAuditEntries(
            Integer page, Integer size, UUID actorId, String action, String entityType,
            UUID entityId, OffsetDateTime from, OffsetDateTime to) {

        Page<AuditEntryDto> trouvees = journal.rechercher(
                actorId, action, entityType, entityId,
                from == null ? null : from.toInstant(),
                to == null ? null : to.toInstant(),
                pagination.de(page, size));

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

    private static AuditEntry versContrat(AuditEntryDto entree) {
        AuditEntry contrat = new AuditEntry();
        contrat.setId(entree.id());
        contrat.setOccurredAt(entree.occurredAt().atOffset(ZoneOffset.UTC));
        contrat.setActorType(AuditEntry.ActorTypeEnum.fromValue(entree.actorType()));
        contrat.setActorId(entree.actorId());
        contrat.setActorLabel(entree.actorLabel());
        contrat.setAction(entree.action());
        contrat.setEntityType(entree.entityType());
        contrat.setEntityId(entree.entityId());
        contrat.setBefore(entree.before());
        contrat.setAfter(entree.after());
        contrat.setReason(entree.reason());
        contrat.setRequestId(entree.requestId());
        return contrat;
    }
}
