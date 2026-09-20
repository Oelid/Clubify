package ma.clubify.platform.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ma.clubify.common.model.entity.ClubScopedEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Défi de second facteur, entre le mot de passe accepté et le code vérifié.
 *
 * <p>Stocké plutôt que signé et rendu au client : un défi doit être à usage
 * unique et révocable.
 */
@Entity
@Table(name = "mfa_challenge")
@Getter
@Setter
public class MfaChallenge extends ClubScopedEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 32)
    private Purpose purpose;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    public boolean estUtilisable(Instant maintenant) {
        return consumedAt == null && expiresAt.isAfter(maintenant);
    }

    public enum Purpose {
        /** Second facteur déjà actif : il faut fournir un code. */
        LOGIN,
        /** Rôle exigeant le second facteur, mais pas encore activé. */
        ENROLLMENT
    }
}
