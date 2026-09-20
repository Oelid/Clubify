package ma.clubify.common.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Socle de toute entité : identifiant UUID, colonnes d'audit, suppression
 * logique (cahier 9.6, CLAUDE.md §3).
 *
 * <p>{@code equals} et {@code hashCode} portent sur l'identifiant seul : les
 * relations paresseuses ne doivent jamais être parcourues par une comparaison
 * (backend/CLAUDE.md).
 */
@MappedSuperclass
@SoftDelete(columnName = "deleted_at", strategy = SoftDeleteType.TIMESTAMP)
@jakarta.persistence.EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    @PrePersist
    void attribuerIdentifiant() {
        if (id == null) {
            id = UuidV7.next();
        }
    }

    @Override
    public final boolean equals(Object autre) {
        if (this == autre) {
            return true;
        }
        if (!(autre instanceof BaseEntity entite)) {
            return false;
        }
        return id != null && Objects.equals(id, entite.getId());
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }
}
