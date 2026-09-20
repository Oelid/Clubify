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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Authentification, second facteur et sessions (SEC-01, décisions 0027 et 0029). */
@IntegrationTest
@DisplayName("Authentification")
class AuthApiTest {

    @Autowired
    private Api api;
    @Autowired
    private TestSeeder seeder;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private Auth auth;
    @Autowired
    private ma.clubify.common.security.TotpService totp;

    private UUID clubA;

    @BeforeEach
    void seed() {
        seeder.reset();
        clubA = seeder.club(Fixtures.CLUB_A);
        seeder.user(clubA, Fixtures.FRONT_DESK_A_EMAIL, "FRONT_DESK", Fixtures.VALID_PASSWORD);
    }

    @Test
    @DisplayName("C5 — bon mot de passe puis mauvais ; seul un haché Argon2 est stocké")
    void c5_motDePasse() throws Exception {
        api.loginRaw(Fixtures.FRONT_DESK_A_EMAIL, Fixtures.VALID_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokens.accessToken").isNotEmpty());

        api.loginRaw(Fixtures.FRONT_DESK_A_EMAIL, "mauvais-mot-de-passe")
                .andExpect(status().isUnauthorized());

        String hash = jdbc.queryForObject(
                "select password_hash from user_account where email = ?",
                String.class, Fixtures.FRONT_DESK_A_EMAIL);
        assertThat(hash).startsWith("$argon2");
        assertThat(hash).doesNotContain(Fixtures.VALID_PASSWORD);
    }

    @Test
    @DisplayName("C5b — 11 caractères refusés, 12 acceptés, sans exigence de caractère spécial")
    void c5b_longueurMinimale() throws Exception {
        String admin = adminToken();

        api.send(admin, post("/api/v1/users"), Map.of(
                        "email", "trop.court@example.test", "firstName", "Prenom", "lastName", "Nom",
                        "role", "FRONT_DESK", "password", Fixtures.TOO_SHORT_PASSWORD))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("user.password.tooShort"));

        api.send(admin, post("/api/v1/users"), Map.of(
                        "email", "assez.long@example.test", "firstName", "Prenom", "lastName", "Nom",
                        "role", "FRONT_DESK", "password", Fixtures.VALID_PASSWORD))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("C6 — second facteur actif : le mot de passe seul ne suffit pas")
    void c6_defiSecondFacteur() throws Exception {
        activer(Fixtures.FRONT_DESK_A_EMAIL);

        api.loginRaw(Fixtures.FRONT_DESK_A_EMAIL, Fixtures.VALID_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("MFA_REQUIRED"))
                .andExpect(jsonPath("$.tokens").doesNotExist())
                .andExpect(jsonPath("$.mfaChallengeId").isNotEmpty());
    }

    @Test
    @DisplayName("C6b — un gérant sans second facteur n'accède à rien avant de l'activer")
    void c6b_enrolementImpose() throws Exception {
        seeder.user(clubA, Fixtures.MANAGER_A_EMAIL, "MANAGER", Fixtures.VALID_PASSWORD);

        String body = api.loginRaw(Fixtures.MANAGER_A_EMAIL, Fixtures.VALID_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("MFA_ENROLLMENT_REQUIRED"))
                .andReturn().getResponse().getContentAsString();

        String token = api.json().readTree(body).path("tokens").path("accessToken").asString();
        // Le jeton délivré à ce stade n'ouvre que l'activation du second facteur.
        api.getAs(token, "/club").andExpect(status().isForbidden());
        api.send(token, post("/api/v1/profile/mfa/setup"), null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("C6c — l'administrateur réinitialise le second facteur d'un utilisateur")
    void c6c_reinitialisation() throws Exception {
        UUID cible = seeder.user(clubA, Fixtures.MANAGER_A_EMAIL, "MANAGER", Fixtures.VALID_PASSWORD);
        activer(Fixtures.MANAGER_A_EMAIL);
        String admin = adminToken();
        long avant = seeder.auditCount();

        api.send(admin, delete("/api/v1/users/" + cible + "/mfa"), null)
                .andExpect(status().isNoContent());

        Boolean actif = jdbc.queryForObject(
                "select mfa_enabled from user_account where id = ?", Boolean.class, cible);
        assertThat(actif).isFalse();
        assertThat(seeder.auditCount()).isGreaterThan(avant);
    }

    @Test
    @DisplayName("C6d — appareil de confiance : plus de code, sauf après expiration")
    void c6d_appareilDeConfiance() throws Exception {
        Auth.Activation activation = activer(Fixtures.FRONT_DESK_A_EMAIL);

        String corps = api.send(null, post("/api/v1/auth/mfa/verify"), Map.of(
                        "mfaChallengeId", defiPour(Fixtures.FRONT_DESK_A_EMAIL),
                        "code", auth.codeCourant(activation.secret()),
                        "trustDevice", true, "deviceLabel", "PC accueil"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String deviceToken = api.json().readTree(corps).path("deviceToken").asString();
        assertThat(deviceToken).isNotBlank();

        api.loginRaw(Fixtures.FRONT_DESK_A_EMAIL, Fixtures.VALID_PASSWORD, deviceToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("AUTHENTICATED"));

        // Au-delà de la durée paramétrée par le club, le code redevient exigé.
        jdbc.update("update trusted_device set expires_at = now() - interval '1 day'");
        api.loginRaw(Fixtures.FRONT_DESK_A_EMAIL, Fixtures.VALID_PASSWORD, deviceToken)
                .andExpect(jsonPath("$.outcome").value("MFA_REQUIRED"));
    }

    @Test
    @DisplayName("C6e — un code de secours ne sert qu'une fois")
    void c6e_codesDeSecours() throws Exception {
        Auth.Activation activation = activer(Fixtures.FRONT_DESK_A_EMAIL);
        String code = activation.codesDeSecours().getFirst();

        api.send(null, post("/api/v1/auth/mfa/verify"), Map.of(
                        "mfaChallengeId", defiPour(Fixtures.FRONT_DESK_A_EMAIL), "code", code))
                .andExpect(status().isOk());

        api.send(null, post("/api/v1/auth/mfa/verify"), Map.of(
                        "mfaChallengeId", defiPour(Fixtures.FRONT_DESK_A_EMAIL), "code", code))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("C8 — le jeton de rafraîchissement ne survit pas à la déconnexion")
    void c8_deconnexion() throws Exception {
        String body = api.loginRaw(Fixtures.FRONT_DESK_A_EMAIL, Fixtures.VALID_PASSWORD)
                .andReturn().getResponse().getContentAsString();
        String access = api.json().readTree(body).path("tokens").path("accessToken").asString();
        String refresh = api.json().readTree(body).path("tokens").path("refreshToken").asString();

        api.send(access, post("/api/v1/auth/logout"), null).andExpect(status().isNoContent());
        api.send(null, post("/api/v1/auth/refresh"), Map.of("refreshToken", refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("C9 — désactiver un utilisateur révoque ses jetons et le conserve en base")
    void c9_desactivation() throws Exception {
        UUID cible = seeder.user(clubA, Fixtures.COACH_A_EMAIL, "COACH", Fixtures.VALID_PASSWORD);
        String jetonCible = api.login(Fixtures.COACH_A_EMAIL, Fixtures.VALID_PASSWORD);

        api.send(adminToken(), put("/api/v1/users/" + cible + "/status"), Map.of("active", false))
                .andExpect(status().isNoContent());

        api.getAs(jetonCible, "/auth/me").andExpect(status().isUnauthorized());
        api.loginRaw(Fixtures.COACH_A_EMAIL, Fixtures.VALID_PASSWORD)
                .andExpect(status().isUnauthorized());
        Integer restant = jdbc.queryForObject(
                "select count(*) from user_account where id = ?", Integer.class, cible);
        assertThat(restant).isEqualTo(1);
    }

    @Test
    @DisplayName("C10 — les échecs sont journalisés sans jamais écrire le mot de passe")
    void c10_echecsJournalises() throws Exception {
        for (int i = 0; i < 3; i++) {
            api.loginRaw(Fixtures.FRONT_DESK_A_EMAIL, "mauvais").andExpect(status().isUnauthorized());
        }

        Integer entrees = jdbc.queryForObject(
                "select count(*) from audit_log where action = 'auth.login.failed'"
                        + " and after_state::text like ?",
                Integer.class, "%" + Fixtures.FRONT_DESK_A_EMAIL + "%");
        assertThat(entrees).isEqualTo(3);

        Integer fuite = jdbc.queryForObject(
                "select count(*) from audit_log where after_state::text like '%mauvais%'",
                Integer.class);
        assertThat(fuite).isZero();
    }

    @Test
    @DisplayName("C10b — cinq échecs verrouillent quinze minutes, même avec le bon mot de passe")
    void c10b_verrouillage() throws Exception {
        for (int i = 0; i < 5; i++) {
            api.loginRaw(Fixtures.FRONT_DESK_A_EMAIL, "mauvais");
        }

        api.loginRaw(Fixtures.FRONT_DESK_A_EMAIL, Fixtures.VALID_PASSWORD)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("auth.account.locked"));

        Integer verrou = jdbc.queryForObject(
                "select count(*) from audit_log where action = 'auth.account.locked'", Integer.class);
        assertThat(verrou).isEqualTo(1);

        // Passé le délai, la connexion redevient possible.
        jdbc.update("update user_account set locked_until = now() - interval '1 minute'");
        api.loginRaw(Fixtures.FRONT_DESK_A_EMAIL, Fixtures.VALID_PASSWORD).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- aides

    private String adminToken() throws Exception {
        seeder.user(clubA, Fixtures.ADMIN_A_EMAIL, "ACCOUNT_ADMIN", Fixtures.VALID_PASSWORD);
        return auth.jetonDe(Fixtures.ADMIN_A_EMAIL);
    }

    private Auth.Activation activer(String email) throws Exception {
        return auth.activer(email);
    }

    private String defiPour(String email) throws Exception {
        return auth.defiPour(email);
    }
}
