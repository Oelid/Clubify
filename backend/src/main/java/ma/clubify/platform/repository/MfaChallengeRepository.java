package ma.clubify.platform.repository;

import ma.clubify.platform.model.entity.MfaChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MfaChallengeRepository extends JpaRepository<MfaChallenge, UUID> {

    @Query("select c from MfaChallenge c where c.expiresAt < :maintenant and c.consumedAt is null")
    List<MfaChallenge> findExpirees(@Param("maintenant") Instant maintenant);

    /** Club d'un défi, hors contexte : l'appelant n'en a pas encore (voir AuthLookup). */
    @Query(value = "select club_id from mfa_challenge where id = :defiId and deleted_at is null",
            nativeQuery = true)
    Optional<UUID> clubDuDefi(@Param("defiId") UUID defiId);

    @Modifying
    @Query("update MfaChallenge c set c.consumedAt = :maintenant "
            + "where c.userId = :userId and c.consumedAt is null")
    int consommerTousPour(@Param("userId") UUID userId, @Param("maintenant") Instant maintenant);
}
