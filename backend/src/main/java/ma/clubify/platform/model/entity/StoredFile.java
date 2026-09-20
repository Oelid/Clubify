package ma.clubify.platform.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ma.clubify.common.model.entity.ClubScopedEntity;

/** Fichier privé, chiffré par l'application avant d'atteindre le support (SEC-03). */
@Entity
@Table(name = "stored_file")
@Getter
@Setter
public class StoredFile extends ClubScopedEntity {

    @Column(name = "purpose", nullable = false, length = 64)
    private String purpose;

    @Column(name = "filename", length = 255)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 160)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    /** Permet la rotation de clé sans réécrire les fichiers existants. */
    @Column(name = "encryption_key_id", nullable = false, length = 64)
    private String encryptionKeyId;
}
