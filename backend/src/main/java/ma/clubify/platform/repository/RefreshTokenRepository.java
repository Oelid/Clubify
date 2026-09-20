package ma.clubify.platform.repository;

import ma.clubify.platform.model.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Ferme toutes les sessions d'un utilisateur sans toucher à son compte (C8b). */
    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :maintenant "
            + "where t.userId = :userId and t.revokedAt is null")
    int revokeAllByUser(@Param("userId") UUID userId, @Param("maintenant") Instant maintenant);
}
