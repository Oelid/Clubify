package ma.clubify.platform.repository;

import ma.clubify.platform.model.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {

    /**
     * Fichier lu depuis un lien signé, hors contexte de club.
     *
     * <p>Le lien a déjà prouvé le droit d'accès : il est opaque, signé et
     * expirant, et il ne désigne qu'un fichier. La requête est native, donc non
     * filtrée, et ne retourne que ce qui est nécessaire pour servir le contenu.
     */
    @Query(value = """
            select id, club_id, filename, content_type, storage_key
            from stored_file where id = :fichierId and deleted_at is null
            """, nativeQuery = true)
    Optional<FichierAServir> parIdSansContexte(@Param("fichierId") UUID fichierId);

    /** Projection minimale : rien de plus que ce que servir le fichier exige. */
    interface FichierAServir {
        UUID getId();

        UUID getClubId();

        String getFilename();

        String getContentType();

        String getStorageKey();

        default UUID id() {
            return getId();
        }

        default UUID clubId() {
            return getClubId();
        }

        default String filename() {
            return getFilename();
        }

        default String contentType() {
            return getContentType();
        }

        default String storageKey() {
            return getStorageKey();
        }
    }
}
