package ma.clubify.platform.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ma.clubify.common.model.entity.ClubScopedEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Code de secours à usage unique, remis à l'activation du second facteur. Sans
 * lui, un administrateur qui perd son téléphone perd son club (benchmark B5).
 */
@Entity
@Table(name = "recovery_code")
@Getter
@Setter
public class RecoveryCode extends ClubScopedEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "used_at")
    private Instant usedAt;
}
