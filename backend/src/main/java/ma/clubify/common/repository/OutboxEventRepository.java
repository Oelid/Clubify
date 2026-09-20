package ma.clubify.common.repository;

import ma.clubify.common.model.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Effets restant à produire, hors contexte de club : le relecteur tourne en
     * fond, il n'appartient à aucun club.
     */
    @Query(value = """
            select * from outbox_event
            where processed_at is null and available_at <= :maintenant and deleted_at is null
            order by available_at
            limit :taille
            """, nativeQuery = true)
    List<OutboxEvent> aTraiter(@Param("maintenant") Instant maintenant,
                               @Param("taille") int taille);
}
