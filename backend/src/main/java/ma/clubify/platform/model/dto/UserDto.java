package ma.clubify.platform.model.dto;

import java.time.Instant;
import java.util.UUID;

/** Utilisateur du club, vu depuis la couche web. */
public record UserDto(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String language,
        String role,
        boolean active,
        boolean mfaEnabled,
        Instant lastLoginAt) {
}
