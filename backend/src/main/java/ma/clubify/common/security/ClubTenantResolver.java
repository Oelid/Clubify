package ma.clubify.common.security;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Fournit à Hibernate le club courant. Toute entité marquée {@code @TenantId}
 * est alors filtrée en lecture et renseignée en écriture, sans qu'aucun service
 * n'ait à y penser (PLT-01).
 */
@Component
public class ClubTenantResolver
        implements CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {

    /**
     * Club employé hors de toute demande : amorçage, migrations, traitements de
     * fond. Aucune donnée réelle ne le porte.
     */
    private static final UUID HORS_CONTEXTE = new UUID(0L, 0L);

    private final TenantContext contexte;

    public ClubTenantResolver(TenantContext contexte) {
        this.contexte = contexte;
    }

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return contexte.find().orElse(HORS_CONTEXTE);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public boolean isRoot(UUID tenantId) {
        // Le club hors contexte ne voit rien : il n'ouvre aucune porte dérobée.
        return false;
    }

    @Override
    public void customize(Map<String, Object> proprietes) {
        proprietes.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
