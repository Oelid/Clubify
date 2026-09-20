package ma.clubify.platform;

import ma.clubify.common.security.PermissionLimits;
import ma.clubify.common.security.TokenService;
import ma.clubify.config.AuthenticatedUser;
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

/**
 * Rôles, permissions et gestion des utilisateurs (SEC-02, décision 0028).
 * Ces cas forment le test de permissions réutilisable du CLAUDE.md §6.
 */
@IntegrationTest
@DisplayName("Utilisateurs, rôles et permissions")
class UsersApiTest {

    @Autowired
    private Api api;
    @Autowired
    private Auth auth;
    @Autowired
    private TestSeeder seeder;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PermissionLimits plafonds;
    @Autowired
    private TokenService jetons;
    @Autowired
    private ma.clubify.common.security.TenantContext contexte;

    private UUID clubA;
    private UUID admin;
    private UUID manager;

    @BeforeEach
    void seed() {
        seeder.reset();
        clubA = seeder.club(Fixtures.CLUB_A);
        admin = seeder.user(clubA, Fixtures.ADMIN_A_EMAIL, "ACCOUNT_ADMIN", Fixtures.VALID_PASSWORD);
        manager = seeder.user(clubA, Fixtures.MANAGER_A_EMAIL, "MANAGER", Fixtures.VALID_PASSWORD);
        seeder.user(clubA, Fixtures.FRONT_DESK_A_EMAIL, "FRONT_DESK", Fixtures.VALID_PASSWORD);
    }

    @Test
    @DisplayName("C40 — la liste se rend par pages, et la base ne renvoie que la page")
    void c40_paginationReelle() throws Exception {
        String admin = adminToken();
        for (int rang = 0; rang < 8; rang += 1) {
            seeder.user(clubA, "coach" + rang + "@exemple.test", "COACH", Fixtures.VALID_PASSWORD);
        }

        api.getAs(admin, "/users?page=0&size=5")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.page").value(0))
                // Le total compte tout le club, pas seulement la page rendue.
                .andExpect(jsonPath("$.page.totalElements").value(11))
                .andExpect(jsonPath("$.page.totalPages").value(3));

        api.getAs(admin, "/users?page=2&size=5")
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.page").value(2));
    }

    @Test
    @DisplayName("C40b — le serveur borne toute taille demandée, quelle qu'elle soit")
    void c40b_plafond() throws Exception {
        String admin = adminToken();

        // Un appel direct à l'API, écrit à la main, ne peut pas obtenir davantage
        // que la borne : elle est posée côté serveur, pas côté client.
        api.getAs(admin, "/users?page=0&size=5000")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(100));

        api.getAs(admin, "/users?page=0&size=100")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(100));

        // En dessous aussi : une page de zéro ligne ne veut rien dire.
        api.getAs(admin, "/users?size=0")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(1));
        api.getAs(admin, "/users?size=-10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(1));

        // Et un numéro de page négatif rend la première page.
        api.getAs(admin, "/users?page=-3&size=20")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.page").value(0));
    }

    @Test
    @DisplayName("C40c — sans taille demandée, celle que le club a retenue s'applique")
    void c40c_tailleDuClub() throws Exception {
        String admin = adminToken();
        seeder.reglage(clubA, "ui.page_size", "30");

        api.getAs(admin, "/users")
                .andExpect(jsonPath("$.page.size").value(30));

        // Et l'interface la reçoit à la connexion, pour ouvrir la liste juste.
        api.getAs(admin, "/auth/me")
                .andExpect(jsonPath("$.club.pageSize").value(30));
    }

    @Test
    @DisplayName("C40d — un club qui demande plus de cent lignes reste borné")
    void c40d_reglageBorne() throws Exception {
        String admin = adminToken();
        seeder.reglage(clubA, "ui.page_size", "500");

        api.getAs(admin, "/users")
                .andExpect(jsonPath("$.page.size").value(100));
    }

    @Test
    @DisplayName("C12c — l'API dit quels rôles sont attribuables, plutôt que l'écran le devine")
    void c12c_rolesAttribuables() throws Exception {
        api.getAs(adminToken(), "/roles")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                // Le parent et le comptable existent au modèle, sans compte avant R8.
                .andExpect(jsonPath("$[?(@.role == 'PARENT')].assignable").value(false))
                .andExpect(jsonPath("$[?(@.role == 'ACCOUNTANT')].assignable").value(false))
                .andExpect(jsonPath("$[?(@.role == 'FRONT_DESK')].assignable").value(true))
                // Et lesquels attendent un second facteur (décisions 0027 et 0031).
                .andExpect(jsonPath("$[?(@.role == 'MANAGER')].requiresSecondFactor").value(true))
                .andExpect(jsonPath("$[?(@.role == 'COACH')].requiresSecondFactor").value(false));
    }

    @Test
    @DisplayName("C41 — la liste se filtre par rôle, par statut et par recherche")
    void c41_filtres() throws Exception {
        String admin = adminToken();
        seeder.user(clubA, "coach.judo@exemple.test", "COACH", Fixtures.VALID_PASSWORD);
        UUID retraite = seeder.user(clubA, "coach.parti@exemple.test", "COACH",
                Fixtures.VALID_PASSWORD);
        api.send(admin, put("/api/v1/users/" + retraite + "/status"),
                Map.of("active", false)).andExpect(status().isNoContent());

        // Par rôle.
        api.getAs(admin, "/users?role=COACH")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2));

        // Par statut : le compte fermé ne compte plus parmi les actifs.
        api.getAs(admin, "/users?role=COACH&active=true")
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("coach.judo@exemple.test"));

        // Par recherche, sans tenir compte de la casse, sur l'adresse comme le nom.
        api.getAs(admin, "/users?search=JUDO")
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("coach.judo@exemple.test"));

        // Une recherche sans résultat rend une page vide, pas une erreur.
        api.getAs(admin, "/users?search=personne-de-ce-nom")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    @DisplayName("C41b — un filtre ne laisse jamais passer un compte d'un autre club")
    void c41b_filtreCloisonne() throws Exception {
        UUID clubB = seeder.club(Fixtures.CLUB_B);
        seeder.user(clubB, "coach.autre-club@exemple.test", "COACH", Fixtures.VALID_PASSWORD);

        // Le filtre s'ajoute au discriminant de club, il ne s'y substitue pas.
        api.getAs(adminToken(), "/users?role=COACH")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    @DisplayName("C7 — aucun compte ne peut exister pour un adhérent mineur")
    void c7_aucunCompteEnfant() throws Exception {
        // Le rôle PARENT existe dans le catalogue mais ne reçoit aucun compte avant R8,
        // et aucun rôle ne permet de créer un compte pour un enfant (section 5, Mineurs).
        api.send(adminToken(), post("/api/v1/users"), Map.of(
                        "email", "enfant@example.test", "firstName", "Prenom", "lastName", "Nom",
                        "role", "PARENT", "password", Fixtures.VALID_PASSWORD))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("user.role.notAssignableYet"));
    }

    @Test
    @DisplayName("C8b — l'administrateur ferme les sessions sans désactiver le compte")
    void c8b_fermetureDesSessions() throws Exception {
        String jetonManager = auth.jetonDe(Fixtures.MANAGER_A_EMAIL);
        long avant = seeder.auditCount();

        api.send(adminToken(), delete("/api/v1/users/" + manager + "/sessions"), null)
                .andExpect(status().isNoContent());

        api.getAs(jetonManager, "/auth/me").andExpect(status().isUnauthorized());
        // Le compte reste actif : il peut se reconnecter.
        api.loginRaw(Fixtures.MANAGER_A_EMAIL, Fixtures.VALID_PASSWORD).andExpect(status().isOk());
        assertThat(seeder.auditCount()).isGreaterThan(avant);
    }

    @Test
    @DisplayName("C11 — l'accueil ne modifie pas les paramètres, même en appelant l'API directement")
    void c11_permissionCoteService() throws Exception {
        String accueil = api.login(Fixtures.FRONT_DESK_A_EMAIL, Fixtures.VALID_PASSWORD);

        api.send(accueil, put("/api/v1/club"), Map.of("name", "Club A Sport"))
                .andExpect(status().isForbidden());

        Integer refus = jdbc.queryForObject(
                "select count(*) from audit_log where action = 'security.access.denied'",
                Integer.class);
        assertThat(refus).isEqualTo(1);
    }

    @Test
    @DisplayName("C11a — une permission retirée puis rendue au gérant, avec trace")
    void c11a_surchargeParUtilisateur() throws Exception {
        String admin_ = adminToken();
        String gerant = auth.jetonDe(Fixtures.MANAGER_A_EMAIL);

        // Par défaut, le gérant peut modifier les paramètres du club.
        api.send(gerant, put("/api/v1/club"), Map.of("name", "Club A Sport"))
                .andExpect(status().isOk());

        api.send(admin_, put("/api/v1/users/" + manager + "/permissions"), List.of(
                        Map.of("code", "club.settings.modifier", "granted", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effective").value(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.hasItem("club.settings.modifier"))));

        String gerant2 = auth.jetonDe(Fixtures.MANAGER_A_EMAIL);
        api.send(gerant2, put("/api/v1/club"), Map.of("name", "Club A Gym"))
                .andExpect(status().isForbidden());

        api.send(admin_, put("/api/v1/users/" + manager + "/permissions"), List.of())
                .andExpect(status().isOk());
        String gerant3 = auth.jetonDe(Fixtures.MANAGER_A_EMAIL);
        api.send(gerant3, put("/api/v1/club"), Map.of("name", "Club A Gym"))
                .andExpect(status().isOk());

        Integer traces = jdbc.queryForObject(
                "select count(*) from audit_log where action = 'user.permissions.updated'",
                Integer.class);
        assertThat(traces).isEqualTo(2);
    }

    @Test
    @DisplayName("C11c — une permission paramétrée respecte son plafond")
    void c11c_permissionParametree() throws Exception {
        String admin = adminToken();
        // Un plafond, à l'image de celui que TAR-05 demandera sur les remises.
        api.send(admin, put("/api/v1/users/" + manager + "/permissions"), List.of(
                        Map.of("code", "club.settings.modifier", "granted", true,
                                "parameter", 100)))
                .andExpect(status().isOk());

        String gerant = auth.jetonDe(Fixtures.MANAGER_A_EMAIL);
        AuthenticatedUser porteur = jetons.lire(gerant);
        assertThat(porteur).as("jeton du gérant lisible").isNotNull();

        // Hors demande servie, le club se pose à la main : c'est ce que fait le
        // filtre d'authentification, et sans quoi le discriminant ne trouve rien.
        contexte.set(clubA);

        // Le plafond se lit depuis la surcharge, et borne la valeur demandée.
        assertThat(plafonds.plafondDe(porteur, "club.settings.modifier").intValue())
                .isEqualTo(100);
        assertThat(plafonds.plafondDe(porteur, "users.consulter"))
                .as("sans paramètre, la permission vaut sans limite")
                .isNull();
        contexte.clear();
    }

    @Test
    @DisplayName("C12 — seul l'administrateur crée un utilisateur, sauf délégation au gérant")
    void c12_creationReservee() throws Exception {
        String accueil = api.login(Fixtures.FRONT_DESK_A_EMAIL, Fixtures.VALID_PASSWORD);
        String gerant = auth.jetonDe(Fixtures.MANAGER_A_EMAIL);
        Map<String, Object> nouveau = Map.of(
                "email", "nouveau@example.test", "firstName", "Prenom", "lastName", "Nom",
                "role", "COACH", "password", Fixtures.VALID_PASSWORD);

        api.send(accueil, post("/api/v1/users"), nouveau).andExpect(status().isForbidden());
        api.send(gerant, post("/api/v1/users"), nouveau).andExpect(status().isForbidden());

        String admin_ = adminToken();
        api.send(admin_, post("/api/v1/users"), nouveau).andExpect(status().isCreated());

        // Après délégation de la gestion des utilisateurs, le gérant y parvient.
        api.send(admin_, put("/api/v1/users/" + manager + "/permissions"), List.of(
                Map.of("code", "users.creer", "granted", true)));
        String gerant2 = auth.jetonDe(Fixtures.MANAGER_A_EMAIL);
        api.send(gerant2, post("/api/v1/users"), Map.of(
                        "email", "second@example.test", "firstName", "Prenom", "lastName", "Nom",
                        "role", "COACH", "password", Fixtures.VALID_PASSWORD))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("C12a — le dernier administrateur ne peut être ni désactivé ni rétrogradé")
    void c12a_dernierAdministrateurProtege() throws Exception {
        String admin_ = adminToken();

        api.send(admin_, put("/api/v1/users/" + admin + "/status"), Map.of("active", false))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("user.lastAdmin.protected"));

        api.send(admin_, put("/api/v1/users/" + admin + "/role"), Map.of("role", "MANAGER"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("user.lastAdmin.protected"));

        // Dès qu'un second administrateur existe, les deux deviennent possibles.
        api.send(admin_, put("/api/v1/users/" + manager + "/role"), Map.of("role", "ACCOUNT_ADMIN"))
                .andExpect(status().isNoContent());
        api.send(admin_, put("/api/v1/users/" + admin + "/role"), Map.of("role", "MANAGER"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("C12b — les droits d'un administrateur ne se retirent pas")
    void c12b_droitsAdministrateurNonRetirables() throws Exception {
        String admin_ = adminToken();
        UUID second = seeder.user(clubA, "admin2.a@example.test", "ACCOUNT_ADMIN",
                Fixtures.VALID_PASSWORD);

        api.send(admin_, put("/api/v1/users/" + second + "/permissions"), List.of(
                        Map.of("code", "club.settings.modifier", "granted", false)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("user.admin.permissionsNotRestrictable"));
    }

    @Test
    @DisplayName("C12 — l'ajustement des permissions n'est jamais délégable")
    void c12_ajustementNonDelegable() throws Exception {
        String admin_ = adminToken();

        api.send(admin_, put("/api/v1/users/" + manager + "/permissions"), List.of(
                        Map.of("code", "users.permissions.modifier", "granted", true)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("security.permission.notDelegable"));
    }

    private String adminToken() throws Exception {
        return auth.jetonDe(Fixtures.ADMIN_A_EMAIL);
    }
}
