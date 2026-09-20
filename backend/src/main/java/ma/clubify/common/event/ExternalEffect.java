package ma.clubify.common.event;

import java.util.Map;
import java.util.UUID;

/**
 * Effet à produire hors de l'application : un message, un export, un appel à un
 * prestataire.
 *
 * <p>Il ne part jamais dans la transaction qui le déclenche : il est déposé dans
 * l'outbox, puis traité. Un échec se rejoue sans rien perdre, et sans jamais
 * empêcher l'action métier d'aboutir (décision 0029).
 */
public record ExternalEffect(UUID clubId, String type, Map<String, String> donnees) {
}
