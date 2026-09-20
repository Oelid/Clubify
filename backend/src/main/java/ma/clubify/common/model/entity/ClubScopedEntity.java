package ma.clubify.common.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

/**
 * Entité rattachée à un club. Hibernate filtre chaque lecture et renseigne
 * chaque écriture depuis le contexte courant : aucun service n'a à passer le
 * club en paramètre, et aucune requête ne peut l'oublier (PLT-01).
 */
@MappedSuperclass
@Getter
@Setter
public abstract class ClubScopedEntity extends BaseEntity {

    @TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    private UUID clubId;
}
