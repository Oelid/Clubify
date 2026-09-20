package ma.clubify.common.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Effet externe à produire : écrit dans la transaction de l'action, traité
 * ensuite, rejoué en cas d'échec. L'audit, lui, est synchrone et bloquant
 * (décision 0029).
 */
@Entity
@Table(name = "outbox_event")
@Getter
@Setter
public class OutboxEvent extends ClubScopedEntity {

    @Column(name = "event_type", nullable = false, length = 160)
    private String eventType;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;
}
