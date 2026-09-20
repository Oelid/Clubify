package ma.clubify.platform.repository;

import ma.clubify.platform.model.entity.UserPermissionOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserPermissionOverrideRepository
        extends JpaRepository<UserPermissionOverride, UUID> {

    List<UserPermissionOverride> findAllByMembershipId(UUID membershipId);

    void deleteAllByMembershipId(UUID membershipId);
}
