package ma.clubify.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Amorce les données de test directement en base.
 *
 * <p>La création du premier club et de son administrateur passe par une commande
 * d'amorçage, pas par l'API (fiche F01, Q1) : les tests reproduisent cet état de
 * départ sans emprunter un chemin qui n'existe pas.
 *
 * <p>Tant que les migrations de F01 ne sont pas écrites, ces insertions échouent :
 * c'est l'état rouge attendu à l'étape 4.
 */
@Component
public class TestSeeder {

    private final JdbcTemplate jdbc;

    public TestSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Crée un club, son site par défaut, et retourne l'identifiant du club. */
    public UUID club(String name) {
        UUID clubId = UUID.randomUUID();
        jdbc.update("""
                insert into club (id, name, timezone, currency, default_language, created_at, updated_at)
                values (?, ?, 'Africa/Casablanca', 'MAD', 'fr', now(), now())
                """, clubId, name);
        jdbc.update("""
                insert into site (id, club_id, name, created_at, updated_at)
                values (?, ?, ?, now(), now())
                """, UUID.randomUUID(), clubId, name);
        return clubId;
    }

    /**
     * Crée un utilisateur et son appartenance au club.
     *
     * @param role ACCOUNT_ADMIN, MANAGER, FRONT_DESK, COACH, ACCOUNTANT
     */
    public UUID user(UUID clubId, String email, String role, String rawPassword) {
        UUID userId = UUID.randomUUID();
        jdbc.update("""
                insert into user_account (id, email, first_name, last_name, password_hash,
                                          language, active, mfa_enabled, created_at, updated_at)
                values (?, ?, 'Prenom', 'Nom', ?, 'fr', true, false, now(), now())
                """, userId, email, PasswordHashes.of(rawPassword));
        jdbc.update("""
                insert into membership (id, club_id, user_id, role, active, created_at, updated_at)
                values (?, ?, ?, ?, true, now(), now())
                """, UUID.randomUUID(), clubId, userId, role);
        return userId;
    }

    public void truncateAll() {
        jdbc.execute("""
                truncate table membership, user_permission_override, refresh_token,
                               trusted_device, recovery_code, club_setting, stored_file,
                               outbox_event, site, user_account, club restart identity cascade
                """);
    }

    /** Le journal d'audit ne se vide jamais par l'application (règle 17). */
    public long auditCount() {
        Long n = jdbc.queryForObject("select count(*) from audit_log", Long.class);
        return n == null ? 0 : n;
    }
}
