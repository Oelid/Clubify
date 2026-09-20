package ma.clubify.platform.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ma.clubify.common.model.entity.ClubScopedEntity;

import java.time.Instant;
import java.util.UUID;

/** Session : le jeton n'est jamais stocké en clair, seulement son empreinte. */
@Entity
@Table(name = "refresh_token")
@Getter
@Setter
public class RefreshToken extends ClubScopedEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @Column(name = "device_label", length = 200)
    private String deviceLabel;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public boolean estUtilisable(Instant maintenant) {
        return revokedAt == null && expiresAt.isAfter(maintenant);
    }
}
