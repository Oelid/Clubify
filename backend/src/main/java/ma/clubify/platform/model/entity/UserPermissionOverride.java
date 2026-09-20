package ma.clubify.platform.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ma.clubify.common.model.entity.ClubScopedEntity;

import java.util.UUID;

/**
 * Surcharge d'une permission pour un utilisateur précis. Le rôle donne le jeu
 * par défaut ; ceci est l'exception, et elle est auditée (décision 0028).
 */
@Entity
@Table(name = "user_permission_override")
@Getter
@Setter
public class UserPermissionOverride extends ClubScopedEntity {

    @Column(name = "membership_id", nullable = false)
    private UUID membershipId;

    @Column(name = "permission_code", nullable = false, length = 120)
    private String permissionCode;

    /** Vrai pour accorder en plus du rôle, faux pour retirer. */
    @Column(name = "granted", nullable = false)
    private boolean granted;

    /** Valeur portée par la permission, tel un plafond de remise (TAR-05). */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "parameter")
    private String parameter;
}
