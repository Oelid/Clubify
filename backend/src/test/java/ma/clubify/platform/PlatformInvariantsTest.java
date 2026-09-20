package ma.clubify.platform;

import ma.clubify.support.Api;
import ma.clubify.support.Fixtures;
import ma.clubify.support.IntegrationTest;
import ma.clubify.support.TestSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Invariants posés une fois pour toutes par F01 (section 9.6, CLAUDE.md §3) :
 * événements, journalisation, i18n, formats, schéma, connecteurs.
 */
@IntegrationTest
@DisplayName("Invariants du socle")
class PlatformInvariantsTest {

    @Autowired
    private Api api;
    @Autowired
    private TestSeeder seeder;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID clubA;

    @BeforeEach
    void seed() {
        seeder.truncateAll();
        clubA = seeder.club(Fixtures.CLUB_A);
        seeder.user(clubA, Fixtures.ADMIN_A_EMAIL, "ACCOUNT_ADMIN", Fixtures.VALID_PASSWORD);
    }

    @Test
    @DisplayName("C15 — aucun téléphone ni donnée personnelle dans les journaux techniques")
    void c15_pasDeDonneePersonnelleDansLesLogs() throws Exception {
        String admin = adminToken();
        api.send(admin, put("/api/v1/club"), Map.of("name", "Club A", "phone", "0612345678"))
                .andExpect(status().isOk());

        String journal = jdbc.queryForObject(
                "select coalesce(string_agg(line, E'\\n'), '') from technical_log_for_test",
                String.class);
        assertThat(journal).doesNotContain("0612345678", "+212612345678");
    }

    @Test
    @DisplayName("C20 — un événement publié dans une transaction annulée n'est jamais consommé")
    void c20_evenementLieALaTransaction() throws Exception {
        long auditAvant = seeder.auditCount();

        // Le point d'entrée de test publie un événement puis échoue volontairement.
        api.send(adminToken(), post("/api/v1/test/echec-apres-publication"), null)
                .andExpect(status().is5xxServerError());

        assertThat(seeder.auditCount()).isEqualTo(auditAvant);
        Integer outbox = jdbc.queryForObject("select count(*) from outbox_event", Integer.class);
        assertThat(outbox).isZero();
    }

    @Test
    @DisplayName("C21 — un effet externe en échec est rejoué et ne s'applique qu'une fois")
    void c21_outboxRejouable() throws Exception {
        api.send(adminToken(), post("/api/v1/test/effet-externe"), null).andExpect(status().isOk());

        jdbc.update("update outbox_event set attempts = 1, last_error = 'panne simulee'");
        jdbc.execute("select replay_outbox_for_test()");
        jdbc.execute("select replay_outbox_for_test()");

        Integer traites = jdbc.queryForObject(
                "select count(*) from outbox_event where processed_at is not null", Integer.class);
        Integer effets = jdbc.queryForObject("select count(*) from test_side_effect", Integer.class);
        assertThat(traites).isEqualTo(1);
        assertThat(effets).isEqualTo(1);
    }

    @Test
    @DisplayName("C22 — un abonné externe défaillant ne bloque pas l'action")
    void c22_abonneExterneNonBloquant() throws Exception {
        jdbc.execute("select fail_next_external_subscriber_for_test()");

        api.send(adminToken(), put("/api/v1/club"), Map.of("name", "Club A Sport"))
                .andExpect(status().isOk());

        assertThat(nomDuClub()).isEqualTo("Club A Sport");
        Integer enErreur = jdbc.queryForObject(
                "select count(*) from outbox_event where last_error is not null", Integer.class);
        assertThat(enErreur).isEqualTo(1);
    }

    @Test
    @DisplayName("C22b — si l'audit échoue, l'action échoue entièrement")
    void c22b_auditBloquant() throws Exception {
        jdbc.execute("select fail_audit_subscriber_for_test()");

        api.send(adminToken(), put("/api/v1/club"), Map.of("name", "Club A Sport"))
                .andExpect(status().is5xxServerError());

        // Décision 0029 : aucune action sur des données sensibles sans trace.
        assertThat(nomDuClub()).isEqualTo(Fixtures.CLUB_A);
        Integer trace = jdbc.queryForObject(
                "select count(*) from audit_log where action = 'club.updated'", Integer.class);
        assertThat(trace).isZero();
    }

    @Test
    @DisplayName("C36 — une erreur porte un code stable et un message FR, jamais un libellé en dur")
    void c36_erreursTraduites() throws Exception {
        String admin = adminToken();

        api.send(admin, put("/api/v1/club"), Map.of("name", ""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("club.name.required"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                // Ni trace technique, ni nom de classe dans la réponse.
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Exception"))));
    }

    @Test
    @DisplayName("C37 — les téléphones sont normalisés en E.164, indicatif +212 par défaut")
    void c37_telephoneE164() throws Exception {
        String admin = adminToken();

        api.send(admin, put("/api/v1/club"), Map.of("name", "Club A", "phone", "06 12 34 56 78"))
                .andExpect(jsonPath("$.phone").value("+212612345678"));

        api.send(admin, put("/api/v1/club"), Map.of("name", "Club A", "phone", "+33 6 12 34 56 78"))
                .andExpect(jsonPath("$.phone").value("+33612345678"));

        api.send(admin, put("/api/v1/club"), Map.of("name", "Club A", "phone", "1234"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("phone.invalid"));
    }

    @Test
    @DisplayName("C38 — chaque table porte club_id, les colonnes d'audit et deleted_at")
    void c38_schemaConforme() {
        List<String> tables = jdbc.queryForList("""
                select table_name from information_schema.tables
                where table_schema = 'public'
                  and table_type = 'BASE TABLE'
                  and table_name not in ('flyway_schema_history', 'club', 'user_account', 'audit_log')
                  and table_name not like '%_for_test'
                """, String.class);
        assertThat(tables).isNotEmpty();

        for (String table : tables) {
            assertThat(colonnes(table))
                    .as("colonnes de %s", table)
                    .contains("club_id", "created_at", "created_by", "updated_at", "updated_by",
                            "deleted_at");
            assertThat(typeDe(table, "id")).as("type de %s.id", table).isEqualTo("uuid");
            assertThat(nonNul(table, "club_id")).as("%s.club_id non nul", table).isTrue();
        }

        // user_account est global : un compte peut appartenir à plusieurs clubs (décision 0029).
        assertThat(colonnes("user_account")).doesNotContain("club_id");
        // audit_log n'accepte un club_id nul que pour les événements d'authentification.
        assertThat(nonNul("audit_log", "club_id")).isFalse();
        Integer contrainte = jdbc.queryForObject("""
                select count(*) from information_schema.table_constraints
                where table_name = 'audit_log' and constraint_type = 'CHECK'
                  and constraint_name = 'audit_log_club_id_required'
                """, Integer.class);
        assertThat(contrainte).isEqualTo(1);
    }

    @Test
    @DisplayName("C39 — sans prestataire configuré, l'implémentation vide reçoit l'appel et le métier passe")
    void c39_connecteursAbstraits() throws Exception {
        api.send(adminToken(), post("/api/v1/test/notifier"), Map.of("code", "test.message"))
                .andExpect(status().isOk());

        Integer appels = jdbc.queryForObject(
                "select count(*) from noop_messaging_call_for_test", Integer.class);
        assertThat(appels).isEqualTo(1);
    }

    // ---------------------------------------------------------------- aides

    private String nomDuClub() {
        return jdbc.queryForObject("select name from club where id = ?", String.class, clubA);
    }

    private List<String> colonnes(String table) {
        return jdbc.queryForList(
                "select column_name from information_schema.columns where table_name = ?",
                String.class, table);
    }

    private String typeDe(String table, String colonne) {
        return jdbc.queryForObject("""
                select data_type from information_schema.columns
                where table_name = ? and column_name = ?
                """, String.class, table, colonne);
    }

    private boolean nonNul(String table, String colonne) {
        String nullable = jdbc.queryForObject("""
                select is_nullable from information_schema.columns
                where table_name = ? and column_name = ?
                """, String.class, table, colonne);
        return "NO".equals(nullable);
    }

    private String adminToken() throws Exception {
        return api.login(Fixtures.ADMIN_A_EMAIL, Fixtures.VALID_PASSWORD);
    }
}
