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
 */
@Component
public class TestSeeder {

    /** Ordre de suppression : les enfants avant les parents. */
    private static final String[] TABLES = {
            "file_link", "outbox_event", "user_permission_override", "refresh_token",
            "mfa_challenge",
            "trusted_device", "recovery_code", "club_setting", "membership",
            "stored_file", "site", "user_account", "club"
    };

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
        UUID userId = jdbc.query(
                "select id from user_account where lower(email) = lower(?) and deleted_at is null",
                rs -> rs.next() ? rs.getObject(1, UUID.class) : null, email);

        if (userId == null) {
            userId = UUID.randomUUID();
            jdbc.update("""
                    insert into user_account (id, email, first_name, last_name, password_hash,
                                              language, active, mfa_enabled, sessions_valid_from,
                                              created_at, updated_at)
                    values (?, ?, 'Prenom', 'Nom', ?, 'fr', true, false, now(), now(), now())
                    """, userId, email, PasswordHashes.of(rawPassword));
        }

        jdbc.update("""
                insert into membership (id, club_id, user_id, role, active, created_at, updated_at)
                values (?, ?, ?, ?, true, now(), now())
                """, UUID.randomUUID(), clubId, userId, role);
        return userId;
    }

    /**
     * Remet la base à blanc entre deux tests.
     *
     * <p>Le journal d'audit refuse toute suppression, y compris par cascade : c'est
     * la règle 17, et le déclencheur la tient même face à un superutilisateur. Le
     * vider ici exige donc de le désactiver explicitement — un geste réservé aux
     * tests, jamais accessible à l'application.
     */
    public void reset() {
        // Le journal part en premier : il référence le club, et comme il ne se
        // supprime jamais, sa clé étrangère bloquerait la suite.
        jdbc.execute("alter table audit_log disable trigger user");
        jdbc.update("delete from audit_log");
        jdbc.execute("alter table audit_log enable trigger user");

        // Le club référence son logo : on délie avant de supprimer les fichiers.
        jdbc.update("update club set logo_file_id = null");

        for (String table : TABLES) {
            jdbc.update("delete from " + table);
        }
    }

    /** Le journal d'audit ne se vide jamais par l'application (règle 17). */
    public long auditCount() {
        Long n = jdbc.queryForObject("select count(*) from audit_log", Long.class);
        return n == null ? 0 : n;
    }
}
