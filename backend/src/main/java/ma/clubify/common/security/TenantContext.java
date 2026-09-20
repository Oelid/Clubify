package ma.clubify.common.security;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Club de la demande en cours. Alimenté par le filtre d'authentification depuis
 * le jeton, jamais depuis la requête du client (PLT-01, contracts/README.md).
 *
 * <p>Hibernate le lit pour filtrer chaque requête ; la base le lit pour sa
 * seconde ligne de défense (Row-Level Security).
 */
@Component
public class TenantContext {

    private static final ThreadLocal<UUID> CLUB = new ThreadLocal<>();

    public void set(UUID clubId) {
        CLUB.set(clubId);
    }

    public Optional<UUID> find() {
        return Optional.ofNullable(CLUB.get());
    }

    /** Le club courant, ou une erreur : aucune donnée métier n'existe hors d'un club. */
    public UUID require() {
        return find().orElseThrow(() ->
                new IllegalStateException("Aucun club dans le contexte : la demande n'est pas authentifiée."));
    }

    public void clear() {
        CLUB.remove();
    }
}
