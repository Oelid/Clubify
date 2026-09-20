package ma.clubify.platform.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ma.clubify.common.model.entity.ClubScopedEntity;

/** Lieu d'exploitation d'un club. Un site par défaut naît avec le club (C3). */
@Entity
@Table(name = "site")
@Getter
@Setter
public class Site extends ClubScopedEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address")
    private String address;
}
