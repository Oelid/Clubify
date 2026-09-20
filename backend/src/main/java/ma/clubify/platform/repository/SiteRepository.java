package ma.clubify.platform.repository;

import ma.clubify.platform.model.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SiteRepository extends JpaRepository<Site, UUID> {

    List<Site> findAllByClubId(UUID clubId);
}
