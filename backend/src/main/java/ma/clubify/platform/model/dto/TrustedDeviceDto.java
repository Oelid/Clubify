package ma.clubify.platform.model.dto;

import java.time.Instant;
import java.util.UUID;

/** Appareil de confiance, vu depuis la couche web. */
public record TrustedDeviceDto(UUID id, String label, Instant lastUsedAt, Instant expiresAt) {
}
