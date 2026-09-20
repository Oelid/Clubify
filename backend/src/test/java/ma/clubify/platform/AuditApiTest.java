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

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Journal d'audit (SEC-04, 9.4 point 12). Ces cas forment le test d'audit
 * réutilisable du CLAUDE.md §6.
 */
@IntegrationTest
@DisplayName("Journal d'audit")
class AuditApiTest {

    @Autowired
    private Api api;
    @Autowired
    private TestSeeder seeder;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID clubA;

    @BeforeEach
    void seed() {
        seeder.reset();
        clubA = seeder.club(Fixtures.CLUB_A);
        seeder.user(clubA, Fixtures.ADMIN_A_EMAIL, "ACCOUNT_ADMIN", Fixtures.VALID_PASSWORD);
        seeder.user(clubA, Fixtures.FRONT_DESK_A_EMAIL, "FRONT_DESK", Fixtures.VALID_PASSWORD);
    }

    @Test
    @DisplayName("C16 — une modification de paramètre trace son avant et son après")
    void c16_avantApres() throws Exception {
        api.send(adminToken(), put("/api/v1/club"), Map.of("name", "Club A Sport"))
                .andExpect(status().isOk());

        Map<String, Object> entree = jdbc.queryForMap("""
                select actor_type, action, entity_type, before_state, after_state, occurred_at, club_id
                from audit_log where action = 'club.updated'
                """);
        assertThat(entree.get("actor_type")).isEqualTo("USER");
        assertThat(entree.get("entity_type")).isEqualTo("Club");
        assertThat(entree.get("before_state").toString()).contains(Fixtures.CLUB_A);
        assertThat(entree.get("after_state").toString()).contains("Club A Sport");
        assertThat(entree.get("club_id")).isEqualTo(clubA);
    }

    @Test
    @DisplayName("C16b — une règle automatique s'inscrit comme auteur « système »")
    void c16b_auteurSysteme() {
        // La règle de test purge les défis de second facteur expirés.
        jdbc.execute("select run_system_rule_for_test('auth.challenge.expire')");

        Map<String, Object> entree = jdbc.queryForMap("""
                select actor_type, actor_id, actor_label from audit_log
                where actor_type = 'SYSTEM' order by occurred_at desc limit 1
                """);
        assertThat(entree.get("actor_id")).isNull();
        assertThat(entree.get("actor_label")).isEqualTo("auth.challenge.expire");
    }

    @Test
    @DisplayName("C17 — le journal refuse toute modification et toute suppression")
    void c17_ajoutSeul() throws Exception {
        api.send(adminToken(), put("/api/v1/club"), Map.of("name", "Club A Sport"));
        assertThat(seeder.auditCount()).isPositive();

        assertThatThrownBy(() -> jdbc.update("update audit_log set action = 'falsifie'"))
                .hasMessageContaining("audit_log");
        assertThatThrownBy(() -> jdbc.update("delete from audit_log"))
                .hasMessageContaining("audit_log");
    }

    @Test
    @DisplayName("C18 — chacune des actions auditées de F01 produit une entrée")
    void c18_couvertureDesActions() throws Exception {
        String admin = adminToken();
        UUID cible = seeder.user(clubA, Fixtures.COACH_A_EMAIL, "COACH", Fixtures.VALID_PASSWORD);

        api.loginRaw(Fixtures.FRONT_DESK_A_EMAIL, "mauvais");
        api.send(admin, put("/api/v1/club"), Map.of("name", "Club A Sport"));
        api.send(admin, put("/api/v1/users/" + cible + "/role"), Map.of("role", "FRONT_DESK"));
        api.send(admin, put("/api/v1/users/" + cible + "/status"), Map.of("active", false));

        assertThat(actions()).contains(
                "auth.login.succeeded", "auth.login.failed", "club.updated",
                "user.role.updated", "user.disabled");
    }

    @Test
    @DisplayName("C19 — l'administrateur et le gérant lisent le journal, l'accueil non")
    void c19_lecteursDuJournal() throws Exception {
        seeder.user(clubA, Fixtures.MANAGER_A_EMAIL, "MANAGER", Fixtures.VALID_PASSWORD);

        api.getAs(adminToken(), "/audit-entries").andExpect(status().isOk());
        api.getAs(api.login(Fixtures.MANAGER_A_EMAIL, Fixtures.VALID_PASSWORD), "/audit-entries")
                .andExpect(status().isOk());
        api.getAs(api.login(Fixtures.FRONT_DESK_A_EMAIL, Fixtures.VALID_PASSWORD), "/audit-entries")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C19b — le journal se filtre par auteur, période, action et entité")
    void c19b_recherche() throws Exception {
        String admin = adminToken();
        api.send(admin, put("/api/v1/club"), Map.of("name", "Club A Sport"));
        api.loginRaw(Fixtures.FRONT_DESK_A_EMAIL, "mauvais");

        api.getAs(admin, "/audit-entries?action=club.updated")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].entityType").value("Club"))
                .andExpect(jsonPath("$.content[0].entityId").isNotEmpty());

        api.getAs(admin, "/audit-entries?from=2100-01-01T00:00:00Z")
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    private java.util.List<String> actions() {
        return jdbc.queryForList("select distinct action from audit_log", String.class);
    }

    private String adminToken() throws Exception {
        return api.login(Fixtures.ADMIN_A_EMAIL, Fixtures.VALID_PASSWORD);
    }
}
