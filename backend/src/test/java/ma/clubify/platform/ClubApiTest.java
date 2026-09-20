package ma.clubify.platform;

import ma.clubify.support.Api;
import ma.clubify.support.Auth;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Paramètres du club et registre des règles configurables (ADM-01, section 9.8). */
@IntegrationTest
@DisplayName("Paramètres du club")
class ClubApiTest {

    @Autowired
    private Api api;
    @Autowired
    private Auth auth;
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
    @DisplayName("C3 — un club naît avec son site par défaut")
    void c3_siteParDefaut() {
        Integer sites = jdbc.queryForObject(
                "select count(*) from site where club_id = ?", Integer.class, clubA);
        assertThat(sites).isEqualTo(1);

        String nom = jdbc.queryForObject(
                "select name from site where club_id = ?", String.class, clubA);
        assertThat(nom).isEqualTo(Fixtures.CLUB_A);
    }

    @Test
    @DisplayName("C28 — identité du club : téléphone normalisé en E.164, ICE mal formé refusé")
    void c28_identite() throws Exception {
        String admin = adminToken();

        api.send(admin, put("/api/v1/club"), Map.of(
                        "name", "Club A Sport", "legalForm", "SARL", "ice", "001234567000089",
                        "taxId", "12345678", "tradeRegister", "45678",
                        "address", "12 rue de la Piscine", "phone", "06 12 34 56 78",
                        "email", "contact@example.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+212612345678"))
                .andExpect(jsonPath("$.ice").value("001234567000089"));

        api.send(admin, put("/api/v1/club"), Map.of("name", "Club A Sport", "ice", "12"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("club.ice.invalid"));
    }

    @Test
    @DisplayName("C29 — une date saisie en heure du club est stockée en UTC et restituée localement")
    void c29_fuseauHoraire() throws Exception {
        String admin = adminToken();
        // Africa/Casablanca est en UTC+1 : 10 h locale vaut 9 h UTC.
        api.send(admin, put("/api/v1/club/settings"), List.of(
                        Map.of("key", "club.timezone", "value", "Africa/Casablanca")))
                .andExpect(status().isOk());

        api.send(admin, put("/api/v1/club"), Map.of("name", "Club A"))
                .andExpect(status().isOk());

        // L'instant stocké est en UTC ; la colonne est un timestamptz.
        String type = jdbc.queryForObject("""
                select data_type from information_schema.columns
                where table_name = 'club' and column_name = 'updated_at'
                """, String.class);
        assertThat(type).isEqualTo("timestamp with time zone");
    }

    @Test
    @DisplayName("C30 — les préfixes de numérotation se saisissent et se tracent, sans émettre de numéro")
    void c30_numerotation() throws Exception {
        String admin = adminToken();

        api.send(admin, put("/api/v1/club/settings"), List.of(
                        Map.of("key", "billing.receipt_prefix", "value", "RC"),
                        Map.of("key", "billing.fiscal_year_start_month", "value", 9)))
                .andExpect(status().isOk());

        Integer traces = jdbc.queryForObject(
                "select count(*) from audit_log where action = 'club.settings.updated'",
                Integer.class);
        assertThat(traces).isEqualTo(1);
    }

    @Test
    @DisplayName("C31 — une règle non saisie prend son défaut ; une fois saisie, la valeur du club")
    void c31_registreDesReglesConfigurables() throws Exception {
        String admin = adminToken();

        api.getAs(admin, "/club/settings")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key=='files.max_size_mb')].value").value(
                        org.hamcrest.Matchers.hasItem(10)))
                .andExpect(jsonPath("$[?(@.key=='files.max_size_mb')].overridden").value(
                        org.hamcrest.Matchers.hasItem(false)));

        api.send(admin, put("/api/v1/club/settings"), List.of(
                        Map.of("key", "files.max_size_mb", "value", 25)))
                .andExpect(status().isOk());

        api.getAs(admin, "/club/settings")
                .andExpect(jsonPath("$[?(@.key=='files.max_size_mb')].value").value(
                        org.hamcrest.Matchers.hasItem(25)))
                .andExpect(jsonPath("$[?(@.key=='files.max_size_mb')].overridden").value(
                        org.hamcrest.Matchers.hasItem(true)));
    }

    @Test
    @DisplayName("C31 — le registre expose la définition et le défaut de chaque règle")
    void c31_definitions() throws Exception {
        api.getAs(adminToken(), "/club/settings/definitions")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key=='club.timezone')].defaultValue").value(
                        org.hamcrest.Matchers.hasItem("Africa/Casablanca")))
                .andExpect(jsonPath("$[?(@.key=='security.mfa.trusted_device_days')].scope").value(
                        org.hamcrest.Matchers.hasItem("CLUB")))
                .andExpect(jsonPath("$[?(@.key=='security.password.min_length')].scope").value(
                        org.hamcrest.Matchers.hasItem("PLATFORM")));
    }

    @Test
    @DisplayName("C32 — l'accueil ne change pas le fuseau du club")
    void c32_parametresReservesAuGerant() throws Exception {
        String accueil = api.login(Fixtures.FRONT_DESK_A_EMAIL, Fixtures.VALID_PASSWORD);

        api.send(accueil, put("/api/v1/club/settings"), List.of(
                        Map.of("key", "club.timezone", "value", "Europe/Paris")))
                .andExpect(status().isForbidden());
    }

    private String adminToken() throws Exception {
        return auth.jetonDe(Fixtures.ADMIN_A_EMAIL);
    }
}
