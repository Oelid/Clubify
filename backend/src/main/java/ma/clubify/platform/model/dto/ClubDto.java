package ma.clubify.platform.model.dto;

import java.util.UUID;

/**
 * Identité du club telle qu'elle franchit la frontière du service.
 *
 * <p>Un contrôleur ne manipule jamais d'entité JPA : elle porte des relations
 * paresseuses et un cycle de vie transactionnel qui n'ont rien à faire dans la
 * couche web (backend/CLAUDE.md, vérifié par ArchitectureTest).
 */
public record ClubDto(
        UUID id,
        String name,
        String legalForm,
        String ice,
        String taxId,
        String tradeRegister,
        String address,
        String phone,
        String email,
        String timezone,
        String currency,
        String defaultLanguage,
        UUID logoFileId) {
}
