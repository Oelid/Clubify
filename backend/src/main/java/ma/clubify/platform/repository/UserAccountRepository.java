package ma.clubify.platform.repository;

import ma.clubify.platform.model.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Les comptes sont globaux : ce dépôt n'est pas filtré par club (décision 0029).
 * C'est {@code MembershipRepository} qui porte le cloisonnement.
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Efface le compteur d'échecs et le verrou après une connexion réussie.
     *
     * <p>Écriture ciblée, hors verrou optimiste : remettre un compteur à zéro
     * n'est pas une décision qui engage quoi que ce soit, et échouer sur un
     * conflit de version renverrait une erreur serveur à quelqu'un qui vient de
     * saisir le bon mot de passe (décision 0030).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserAccount c set c.failedAttempts = 0, c.lockedUntil = null where c.id = :id")
    void effacerLesEchecs(@Param("id") UUID id);

    /**
     * Date la connexion qui vient de s'ouvrir.
     *
     * <p>Écriture ciblée, pour la même raison : une même personne connectée au
     * comptoir et sur son téléphone ferait, sinon, échouer l'une des deux sur un
     * conflit de version. Dater une connexion n'est pas une décision à protéger
     * par un verrou (décision 0030).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserAccount c set c.lastLoginAt = :instant where c.id = :id")
    void marquerLaConnexion(@Param("id") UUID id, @Param("instant") Instant instant);
}
