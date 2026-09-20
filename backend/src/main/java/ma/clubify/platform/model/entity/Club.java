package ma.clubify.platform.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import ma.clubify.common.model.entity.BaseEntity;

import java.util.UUID;

/**
 * Le tenant. Elle ne porte pas de {@code club_id} : elle est le club.
 */
@Entity
@Table(name = "club")
@Getter
@Setter
public class Club extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "legal_form", length = 100)
    private String legalForm;

    @Column(name = "ice", length = 15)
    private String ice;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "trade_register", length = 50)
    private String tradeRegister;

    @Column(name = "address")
    private String address;

    /** Format E.164, indicatif +212 par défaut (section 5). */
    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 320)
    private String email;

    /** Sert à l'affichage et aux règles calendaires, jamais au stockage. */
    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "default_language", nullable = false, length = 8)
    private String defaultLanguage;

    @Column(name = "logo_file_id")
    private UUID logoFileId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
