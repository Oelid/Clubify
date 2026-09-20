package ma.clubify.platform.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ma.clubify.common.model.entity.ClubScopedEntity;

import java.time.Instant;
import java.util.UUID;

/** Lien signé et expirant vers un fichier (PLT-05, critère C23). */
@Entity
@Table(name = "file_link")
@Getter
@Setter
public class FileLink extends ClubScopedEntity {

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
