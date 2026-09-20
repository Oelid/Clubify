package ma.clubify.platform.repository;

import ma.clubify.platform.model.entity.FileLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface FileLinkRepository extends JpaRepository<FileLink, UUID> {

    /** Lien lu hors contexte de club : c'est lui qui désigne le club (voir FileService). */
    @Query(value = """
            select file_id, club_id, expires_at from file_link
            where token_hash = :empreinte and deleted_at is null
            """, nativeQuery = true)
    Optional<LienServi> parEmpreinte(@Param("empreinte") String empreinte);

    interface LienServi {
        UUID getFileId();

        UUID getClubId();

        Instant getExpiresAt();

        default UUID fileId() {
            return getFileId();
        }

        default UUID clubId() {
            return getClubId();
        }

        default Instant expiresAt() {
            return getExpiresAt();
        }
    }
}
