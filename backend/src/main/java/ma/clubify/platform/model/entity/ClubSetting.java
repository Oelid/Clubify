package ma.clubify.platform.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ma.clubify.common.model.entity.ClubScopedEntity;

/**
 * Valeur d'une règle configurable par club (section 9.8). Le défaut vit dans le
 * registre en code : cette table ne porte que ce que le club a saisi.
 */
@Entity
@Table(name = "club_setting")
@Getter
@Setter
public class ClubSetting extends ClubScopedEntity {

    @Column(name = "setting_key", nullable = false, length = 120)
    private String settingKey;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "value", nullable = false)
    private String value;
}
