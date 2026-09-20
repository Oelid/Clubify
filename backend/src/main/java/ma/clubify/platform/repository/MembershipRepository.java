package ma.clubify.platform.repository;

import ma.clubify.platform.model.entity.Membership;
import ma.clubify.platform.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Toutes ces requêtes sont filtrées par club : Hibernate applique le
 * discriminant, aucune méthode n'a donc à prendre un club en paramètre (PLT-01).
 */
public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findByUserId(UUID userId);

    List<Membership> findAllByRole(Role role);

    long countByRoleAndActiveTrue(Role role);
}
