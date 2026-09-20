package ma.clubify.platform.repository;

import ma.clubify.platform.model.entity.Membership;
import ma.clubify.platform.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Toutes ces requêtes sont filtrées par club : Hibernate applique le
 * discriminant, aucune méthode n'a donc à prendre un club en paramètre (PLT-01).
 *
 * <p>Une seule exception, {@link #findForAuthentication(UUID)}, et elle est de
 * nature : à la connexion, le club n'est pas encore connu — c'est justement ce
 * que la requête cherche.
 */
public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findByUserId(UUID userId);

    List<Membership> findAllByRole(Role role);

    long countByRoleAndActiveTrue(Role role);

    /**
     * Appartenance d'un utilisateur, hors de tout contexte de club.
     *
     * <p>Requête native, donc non filtrée par le discriminant : c'est
     * indispensable et sans danger, puisqu'elle ne rend que les appartenances du
     * compte qui vient de prouver son identité. Le club qu'elle retourne est
     * ensuite posé dans le jeton, et tout le reste de l'application est filtré.
     *
     * <p>Aucun autre chemin ne doit contourner le discriminant.
     */
    @Query(value = """
            select * from membership
            where user_id = :userId and active = true and deleted_at is null
            order by created_at
            limit 1
            """, nativeQuery = true)
    Optional<Membership> findForAuthentication(@Param("userId") UUID userId);
}
