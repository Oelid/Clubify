package ma.clubify.platform.repository;

import ma.clubify.platform.model.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Les comptes sont globaux : ce dépôt n'est pas filtré par club (décision 0029).
 * C'est {@code MembershipRepository} qui porte le cloisonnement.
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
