package ma.clubify.common.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Entrée du journal d'audit (SEC-04).
 *
 * <p>Elle n'hérite pas de {@link BaseEntity} : une ligne d'audit n'a ni date de
 * modification ni suppression logique, puisqu'elle ne se corrige jamais. Elle ne
 * porte pas non plus le discriminant de club, parce qu'une tentative de
 * connexion sur un identifiant inconnu n'appartient à aucun club (décision 0029).
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
public class AuditLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "club_id")
    private UUID clubId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 16)
    private ActorType actorType;

    @Column(name = "actor_id")
    private UUID actorId;

    /** Nom de l'utilisateur, ou identifiant de la règle automatique. */
    @Column(name = "actor_label", length = 200)
    private String actorLabel;

    @Column(name = "action", nullable = false, length = 160)
    private String action;

    @Column(name = "entity_type", length = 120)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state")
    private String beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state")
    private String afterState;

    @Column(name = "reason")
    private String reason;

    @Column(name = "request_id", length = 64)
    private String requestId;

    /** Qui a agi : un utilisateur, un parent depuis le portail, ou le système. */
    public enum ActorType {
        USER, PARENT, SYSTEM
    }
}
