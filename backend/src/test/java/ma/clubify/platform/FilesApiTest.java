package ma.clubify.platform;

import ma.clubify.support.Api;
import ma.clubify.support.Fixtures;
import ma.clubify.support.IntegrationTest;
import ma.clubify.support.TestSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fichiers privés, chiffrés, servis par lien temporaire (PLT-05, SEC-03). */
@IntegrationTest
@DisplayName("Fichiers privés")
class FilesApiTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};

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
    }

    @Test
    @DisplayName("C13 — le contenu est illisible sur le support et restitué en clair par l'application")
    void c13_chiffrementAuRepos() throws Exception {
        String admin = adminToken();
        UUID fichier = deposerLogo(admin);

        byte[] stocke = contenuBrutSurLeSupport(fichier);
        assertThat(stocke).isNotEqualTo(PNG);

        String lien = lienDe(admin, fichier);
        byte[] servi = api.mvc().perform(get(lien))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(servi).isEqualTo(PNG);
    }

    @Test
    @DisplayName("C14 — chaque accès à un fichier laisse une trace")
    void c14_accesJournalise() throws Exception {
        String admin = adminToken();
        UUID fichier = deposerLogo(admin);

        api.mvc().perform(get(lienDe(admin, fichier))).andExpect(status().isOk());

        Map<String, Object> entree = jdbc.queryForMap("""
                select actor_type, action, entity_id, occurred_at from audit_log
                where action = 'file.accessed' order by occurred_at desc limit 1
                """);
        assertThat(entree.get("entity_id")).isEqualTo(fichier);
        assertThat(entree.get("occurred_at")).isNotNull();
    }

    @Test
    @DisplayName("C23 — un lien expire au bout de la durée paramétrée")
    void c23_lienTemporaire() throws Exception {
        String admin = adminToken();
        UUID fichier = deposerLogo(admin);
        String lien = lienDe(admin, fichier);

        api.mvc().perform(get(lien)).andExpect(status().isOk());

        // Le lien porte sa propre échéance ; passée celle-ci, il n'ouvre plus rien.
        jdbc.update("update file_link set expires_at = now() - interval '1 minute'");
        api.mvc().perform(get(lien)).andExpect(status().isGone());

        api.mvc().perform(get(lienDe(admin, fichier))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("C24 — un lien valide obtenu dans un club n'ouvre rien depuis un autre")
    void c24_lienCloisonne() throws Exception {
        UUID clubB = seeder.club(Fixtures.CLUB_B);
        seeder.user(clubB, Fixtures.ADMIN_B_EMAIL, "ACCOUNT_ADMIN", Fixtures.VALID_PASSWORD);

        UUID fichierA = deposerLogo(adminToken());
        String jetonB = api.login(Fixtures.ADMIN_B_EMAIL, Fixtures.VALID_PASSWORD);

        api.getAs(jetonB, "/files/" + fichierA + "/link").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("C25 — taille et type sont contrôlés selon les valeurs du club")
    void c25_tailleEtType() throws Exception {
        String admin = adminToken();

        byte[] trop = new byte[11 * 1024 * 1024];
        api.mvc().perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", "gros.pdf", "application/pdf", trop))
                        .param("purpose", "CLUB_LOGO")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("file.size.tooLarge"));

        api.mvc().perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", "outil.exe",
                                "application/octet-stream", new byte[]{1}))
                        .param("purpose", "CLUB_LOGO")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("file.type.notAllowed"));

        api.mvc().perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", "logo.png", MediaType.IMAGE_PNG_VALUE, PNG))
                        .param("purpose", "CLUB_LOGO")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("C26 — supprimer un fichier le retire du service sans effacer sa ligne")
    void c26_suppressionLogique() throws Exception {
        String admin = adminToken();
        UUID fichier = deposerLogo(admin);

        api.send(admin, delete("/api/v1/files/" + fichier), null).andExpect(status().isNoContent());

        api.getAs(admin, "/files/" + fichier + "/link").andExpect(status().isNotFound());
        Map<String, Object> ligne = jdbc.queryForMap(
                "select deleted_at from stored_file where id = ?", fichier);
        assertThat(ligne.get("deleted_at")).isNotNull();
    }

    @Test
    @DisplayName("C27 — le logo du club est un fichier privé, quel que soit le support de stockage")
    void c27_logoDuClub() throws Exception {
        String admin = adminToken();
        UUID fichier = deposerLogo(admin);

        api.send(admin, put("/api/v1/club/logo"), Map.of("fileId", fichier.toString()))
                .andExpect(status().isNoContent());

        api.getAs(admin, "/club")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoFileId").value(fichier.toString()));
    }

    // ---------------------------------------------------------------- aides

    private UUID deposerLogo(String token) throws Exception {
        String body = api.mvc().perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", "logo.png", MediaType.IMAGE_PNG_VALUE, PNG))
                        .param("purpose", "CLUB_LOGO")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(api.json().readTree(body).path("id").asString());
    }

    private String lienDe(String token, UUID fichier) throws Exception {
        String body = api.getAs(token, "/files/" + fichier + "/link")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return api.json().readTree(body).path("url").asString();
    }

    /** Lit l'octet-à-octet tel qu'il est conservé, sans passer par l'application. */
    private byte[] contenuBrutSurLeSupport(UUID fichier) {
        List<byte[]> contenus = jdbc.query(
                "select raw_content_for_test(storage_key) from stored_file where id = ?",
                (rs, i) -> rs.getBytes(1), fichier);
        assertThat(contenus).hasSize(1);
        return contenus.getFirst();
    }

    private String adminToken() throws Exception {
        return api.login(Fixtures.ADMIN_A_EMAIL, Fixtures.VALID_PASSWORD);
    }

    @SuppressWarnings("unused")
    private static String utf8(byte[] b) {
        return new String(b, StandardCharsets.UTF_8);
    }
}
