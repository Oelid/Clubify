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

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exports de listes (INT-03), colonnes sensibles exclues (SEC-03, benchmark B7). */
@IntegrationTest
@DisplayName("Exports")
class ExportsApiTest {

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
        UUID clubB = seeder.club(Fixtures.CLUB_B);
        seeder.user(clubA, Fixtures.ADMIN_A_EMAIL, "ACCOUNT_ADMIN", Fixtures.VALID_PASSWORD);
        seeder.user(clubA, Fixtures.COACH_A_EMAIL, "COACH", Fixtures.VALID_PASSWORD);
        seeder.user(clubB, Fixtures.ADMIN_B_EMAIL, "ACCOUNT_ADMIN", Fixtures.VALID_PASSWORD);
    }

    @Test
    @DisplayName("C33 — CSV et Excel donnent les mêmes lignes, en-têtes en FR, sans fuite entre clubs")
    void c33_csvEtExcel() throws Exception {
        String admin = adminToken();

        String csv = new String(api.send(admin, post("/api/v1/exports"),
                                Map.of("dataset", "users", "format", "CSV"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertThat(csv).contains("Courriel", "Rôle");
        assertThat(csv).contains(Fixtures.ADMIN_A_EMAIL, Fixtures.COACH_A_EMAIL);
        assertThat(csv).doesNotContain(Fixtures.ADMIN_B_EMAIL);

        byte[] xlsx = api.send(admin, post("/api/v1/exports"),
                        Map.of("dataset", "users", "format", "XLSX"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        // Un xlsx est une archive ZIP : les deux premiers octets le disent.
        assertThat(xlsx).startsWith(new byte[]{'P', 'K'});

        long lignesCsv = csv.lines().filter(l -> !l.isBlank()).count() - 1;
        assertThat(lignesCsv).isEqualTo(2);
    }

    @Test
    @DisplayName("C33b — consulter une liste n'emporte pas le droit de l'exporter")
    void c33b_exporterEstUnDroitDistinct() throws Exception {
        String coach = api.login(Fixtures.COACH_A_EMAIL, Fixtures.VALID_PASSWORD);

        api.send(coach, post("/api/v1/exports"), Map.of("dataset", "users", "format", "CSV"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("C34 — aucune colonne sensible ne figure dans un export, quel que soit le rôle")
    void c34_colonnesSensiblesExclues() throws Exception {
        String admin = adminToken();

        String csv = new String(api.send(admin, post("/api/v1/exports"),
                        Map.of("dataset", "users", "format", "CSV"))
                .andReturn().getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        // SEC-03 : santé, CIN et pièces ne sortent jamais. Le haché de mot de passe
        // et le secret du second facteur non plus.
        assertThat(csv).doesNotContain("password", "mfa_secret", "$argon2");
    }

    @Test
    @DisplayName("C35 — chaque export laisse sa trace : qui, quelle liste, combien de lignes")
    void c35_exportJournalise() throws Exception {
        api.send(adminToken(), post("/api/v1/exports"),
                        Map.of("dataset", "users", "format", "CSV"))
                .andExpect(status().isOk());

        Map<String, Object> entree = jdbc.queryForMap("""
                select actor_type, action, after_state from audit_log
                where action = 'export.created' order by occurred_at desc limit 1
                """);
        assertThat(entree.get("actor_type")).isEqualTo("USER");
        String details = entree.get("after_state").toString();
        assertThat(details).contains("users").contains("CSV").contains("2");
    }

    private String adminToken() throws Exception {
        return api.login(Fixtures.ADMIN_A_EMAIL, Fixtures.VALID_PASSWORD);
    }
}
