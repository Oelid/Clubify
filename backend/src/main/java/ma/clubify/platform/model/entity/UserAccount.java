package ma.clubify.platform.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import ma.clubify.common.model.entity.BaseEntity;
import ma.clubify.common.security.EncryptedStringConverter;

import java.time.Instant;

/**
 * Compte du staff. Volontairement sans {@code club_id} : un même compte peut
 * appartenir à plusieurs clubs (PLT-02, décision 0029). C'est {@link Membership}
 * qui porte le club et le rôle.
 */
@Entity
@Table(name = "user_account")
@Getter
@Setter
public class UserAccount extends BaseEntity {

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "language", nullable = false, length = 8)
    private String language;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    /** Chiffré au repos : le support ne voit jamais le clair (SEC-03). */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * Tout jeton d'accès émis avant cette borne est refusé. La déplacer révoque
     * instantanément les sessions, sans attendre l'échéance des jetons.
     */
    @Column(name = "sessions_valid_from", nullable = false)
    private Instant sessionsValidFrom;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public boolean estVerrouille(Instant maintenant) {
        return lockedUntil != null && lockedUntil.isAfter(maintenant);
    }
}
