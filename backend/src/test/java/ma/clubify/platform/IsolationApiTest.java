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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Isolation stricte par club (PLT-01). Ces cas forment le test d'isolation
 * réutilisable exigé par le CLAUDE.md §6 : toute feature touchant à l'argent,
 * aux remises, aux dérogations ou aux données sensibles le rejoue.
 */
@IntegrationTest
@DisplayName("Isolation par club")
class IsolationApiTest {

    @Autowired
    private Api api;
    @Autowired
    private Auth auth;
    @Autowired
    private TestSeeder seeder;

    private UUID clubA;
    private UUID userB;

    @BeforeEach
    void seed() {
        seeder.reset();
        clubA = seeder.club(Fixtures.CLUB_A);
        UUID clubB = seeder.club(Fixtures.CLUB_B);
        seeder.user(clubA, Fixtures.ADMIN_A_EMAIL, "ACCOUNT_ADMIN", Fixtures.VALID_PASSWORD);
        userB = seeder.user(clubB, Fixtures.ADMIN_B_EMAIL, "ACCOUNT_ADMIN", Fixtures.VALID_PASSWORD);
    }

    @Test
    @DisplayName("C1 — la liste des utilisateurs ne montre que ceux du club connecté")
    void c1_listeCloisonnee() throws Exception {
        String token = auth.jetonDe(Fixtures.ADMIN_A_EMAIL);

        api.getAs(token, "/users")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].email").value(
                        org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.not(Fixtures.ADMIN_B_EMAIL))));
    }

    @Test
    @DisplayName("C1 — un identifiant d'un autre club répond « introuvable », jamais « interdit »")
    void c1_pasDeFuiteParLeCodeDErreur() throws Exception {
        String token = auth.jetonDe(Fixtures.ADMIN_A_EMAIL);

        api.getAs(token, "/users/" + userB).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("C2 — un club_id envoyé par le client est ignoré")
    void c2_clubIdDuClientIgnore() throws Exception {
        String token = auth.jetonDe(Fixtures.ADMIN_A_EMAIL);

        // Le contrat ne prévoit aucun paramètre de club : s'il en arrive un, il ne
        // doit rien changer. La réponse reste celle du club du jeton.
        String avec = api.mvc().perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .param("clubId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String sans = api.getAs(token, "/users")
                .andReturn().getResponse().getContentAsString();

        assertThat(avec).isEqualTo(sans);
    }

    @Test
    @DisplayName("C4 — un utilisateur rattaché à deux clubs n'a que les droits du club connecté")
    void c4_rolesParClub() throws Exception {
        UUID clubB2 = seeder.club("Club B bis");
        // Même personne, deux appartenances : coach chez A, administrateur chez B bis.
        seeder.user(clubA, Fixtures.COACH_A_EMAIL, "COACH", Fixtures.VALID_PASSWORD);
        seeder.user(clubB2, Fixtures.COACH_A_EMAIL, "ACCOUNT_ADMIN", Fixtures.VALID_PASSWORD);

        String token = api.login(Fixtures.COACH_A_EMAIL, Fixtures.VALID_PASSWORD);

        api.getAs(token, "/auth/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("COACH"))
                .andExpect(jsonPath("$.club.name").value(Fixtures.CLUB_A));
        api.getAs(token, "/users").andExpect(status().isForbidden());
    }
}
