package ma.clubify.common.repository;

import ma.clubify.common.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Le journal ne s'écrit qu'en ajout : aucune méthode de modification ni de
 * suppression n'est exposée, et la base refuse les deux de toute façon (C17).
 */
public interface AuditLogRepository
        extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {
}
