package ma.clubify.platform.model.dto;

import java.time.Instant;
import java.util.UUID;

/** Entrée du journal d'audit, vue depuis la couche web. */
public record AuditEntryDto(
        UUID id,
        Instant occurredAt,
        String actorType,
        UUID actorId,
        String actorLabel,
        String action,
        String entityType,
        UUID entityId,
        String before,
        String after,
        String reason,
        String requestId) {
}
