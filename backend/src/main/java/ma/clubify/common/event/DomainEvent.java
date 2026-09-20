package ma.clubify.common.event;

import java.util.UUID;

/**
 * Fait métier publié une fois (PLT-04). L'audit s'y abonne de façon synchrone
 * et bloquante ; les effets externes passent par l'outbox (décision 0029).
 *
 * @param clubId      club concerné ; nul pour un événement d'authentification
 *                    sur un identifiant inconnu
 * @param action      code stable, au format domaine.objet.participe-passé
 * @param entityType  type de l'entité touchée, pour la recherche dans le journal
 * @param entityId    identifiant de l'entité touchée
 * @param before      état avant, sérialisé ; nul à la création
 * @param after       état après, sérialisé ; nul à la suppression
 * @param reason      motif, quand une règle l'exige (remise, dérogation)
 */
public record DomainEvent(
        UUID clubId,
        String action,
        String entityType,
        UUID entityId,
        String before,
        String after,
        String reason) {

    public static DomainEvent of(UUID clubId, String action, String entityType, UUID entityId) {
        return new DomainEvent(clubId, action, entityType, entityId, null, null, null);
    }

    public static DomainEvent modification(UUID clubId, String action, String entityType,
                                           UUID entityId, String before, String after) {
        return new DomainEvent(clubId, action, entityType, entityId, before, after, null);
    }
}
