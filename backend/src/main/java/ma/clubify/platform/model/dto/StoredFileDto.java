package ma.clubify.platform.model.dto;

import java.time.Instant;
import java.util.UUID;

/** Fichier déposé, vu depuis la couche web. */
public record StoredFileDto(
        UUID id,
        String purpose,
        String filename,
        String contentType,
        long sizeBytes,
        Instant createdAt) {
}
