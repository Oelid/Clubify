package ma.clubify.platform;

import ma.clubify.common.audit.AuditFailureSwitch;
import ma.clubify.common.audit.SystemActor;
import ma.clubify.common.connector.MessagingProvider;
import ma.clubify.common.connector.NoopMessagingProvider;
import ma.clubify.common.connector.PaymentProvider;
import ma.clubify.common.event.DomainEvent;
import ma.clubify.common.event.DomainEvents;
import ma.clubify.common.event.ExternalEffect;
import ma.clubify.common.event.OutboxPublisher;
import ma.clubify.common.event.OutboxRelay;
import ma.clubify.common.model.Money;
import ma.clubify.support.Api;
import ma.clubify.support.Auth;
import ma.clubify.support.Fixtures;
import ma.clubify.support.IntegrationTest;
import ma.clubify.support.LogCapture;
import ma.clubify.support.TestSeeder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Invariants posés une fois pour toutes par F01 (section 9.6, CLAUDE.md §3) :
 * événements, journalisation, i18n, formats, schéma, connecteurs.
 *
 * <p>Ceux qui ne se voient pas depuis l'API sont éprouvés en appelant le socle
 * directement. C'est un choix assumé : ouvrir des points d'entrée de test
 * percerait l'application pour la commodité des tests.
 */
@IntegrationTest
@DisplayName("Invariants du socle")
class PlatformInvariantsTest {

    @Autowired
    private Api api;
    @Autowired
    private Auth auth;
    @Autowired
    private TestSeeder seeder;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TransactionTemplate transaction;
    @Autowired
    private DomainEvents evenements;
    @Autowired
    private OutboxPublisher outbox;
    @Autowired
    private OutboxRelay relais;
    @Autowired
    private AuditFailureSwitch interrupteur;
    @Autowired
    private MessagingProvider messagerie;
    @Autowired
    private PaymentProvider paiement;
    @Autowired
    private ma.clubify.common.security.TenantContext contexte;

    private UUID clubA;

    @BeforeEach
    void seed() {
        seeder.reset();
        clubA = seeder.club(Fixtures.CLUB_A);
        seeder.user(clubA, Fixtures.ADMIN_A_EMAIL, "ACCOUNT_ADMIN", Fixtures.VALID_PASSWORD);
        // Hors demande servie, le club se pose à la main : c'est ce que fait
        // le filtre d'authentification dans la vraie vie.
        contexte.set(clubA);
    }

    @AfterEach
    void desarmer() {
        interrupteur.desarmer();
        contexte.clear();
    }

    @Test
    @DisplayName("C15 — aucun téléphone ni donnée personnelle dans les journaux techniques")
    void c15_pasDeDonneePersonnelleDansLesLogs() throws Exception {
        String admin = adminToken();

        try (LogCapture journal = LogCapture.sur("ma.clubify")) {
            api.send(admin, put("/api/v1/club"),
                            Map.of("name", "Club A", "phone", "0612345678"))
                    .andExpect(status().isOk());

            assertThat(journal.texte())
                    .doesNotContain("0612345678")
                    .doesNotContain("+212612345678")
                    .doesNotContain(Fixtures.ADMIN_A_EMAIL);
        }
    }

    @Test
    @DisplayName("C20 — un effet publié dans une transaction annulée n'est jamais produit")
    void c20_evenementLieALaTransaction() {
        long auditAvant = seeder.auditCount();

        assertThatThrownBy(() -> transaction.executeWithoutResult(statut -> {
            evenements.publish(DomainEvent.of(clubA, "test.fait", "Club", clubA));
            outbox.deposer(new ExternalEffect(clubA, "test.effet", Map.of()));
            throw new IllegalStateException("échec volontaire après publication");
        })).isInstanceOf(IllegalStateException.class);

        // Ni trace d'audit, ni effet en attente : le fait métier n'a pas eu lieu.
        assertThat(seeder.auditCount()).isEqualTo(auditAvant);
        assertThat(comptable("outbox_event")).isZero();
    }

    @Test
    @DisplayName("C21 — un effet en attente est produit une fois, même rejoué")
    void c21_outboxRejouable() {
        transaction.executeWithoutResult(statut ->
                outbox.deposer(new ExternalEffect(clubA, "test.effet", Map.of())));
        assertThat(comptable("outbox_event")).isEqualTo(1);

        int premier = relais.traiterUnLot();
        int second = relais.traiterUnLot();

        assertThat(premier).isEqualTo(1);
        assertThat(second).as("un effet déjà produit ne se reproduit pas").isZero();
        assertThat(comptableOu("outbox_event", "processed_at is not null")).isEqualTo(1);
    }

    @Test
    @DisplayName("C22 — un effet externe en attente ne remonte jamais à l'action")
    void c22_effetExterneNonBloquant() throws Exception {
        String admin = adminToken();

        api.send(admin, put("/api/v1/club"), Map.of("name", "Club A Sport"))
                .andExpect(status().isOk());

        // La demande servie a vidé le contexte en sortant : on le repose.
        contexte.set(clubA);
        transaction.executeWithoutResult(statut ->
                outbox.deposer(new ExternalEffect(clubA, "test.effet.defaillant", Map.of())));

        // L'action reste acquise, que l'effet aboutisse ou non.
        assertThat(nomDuClub()).isEqualTo("Club A Sport");
        assertThat(comptable("outbox_event")).isEqualTo(1);
    }

    @Test
    @DisplayName("C22b — si l'audit échoue, l'action échoue entièrement")
    void c22b_auditBloquant() throws Exception {
        String admin = adminToken();
        interrupteur.armer();

        api.send(admin, put("/api/v1/club"), Map.of("name", "Club A Sport"))
                .andExpect(status().is5xxServerError());

        // Décision 0029 : aucune action sur des données sensibles sans trace.
        assertThat(nomDuClub()).isEqualTo(Fixtures.CLUB_A);
        assertThat(comptableOu("audit_log", "action = 'club.updated'")).isZero();
    }

    @Test
    @DisplayName("C16b — une règle automatique s'inscrit comme auteur « système »")
    void c16b_auteurSysteme() {
        SystemActor.executer("test.regle.automatique", () ->
                transaction.executeWithoutResult(statut ->
                        evenements.publish(DomainEvent.of(
                                clubA, "test.regle.automatique", "Club", clubA))));

        Map<String, Object> entree = jdbc.queryForMap("""
                select actor_type, actor_id, actor_label from audit_log
                where action = 'test.regle.automatique' order by occurred_at desc limit 1
                """);
        assertThat(entree.get("actor_type")).isEqualTo("SYSTEM");
        assertThat(entree.get("actor_id")).as("une règle n'est pas une personne").isNull();
        assertThat(entree.get("actor_label")).isEqualTo("test.regle.automatique");
    }

    @Test
    @DisplayName("C36 — une erreur porte un code stable et un message FR, jamais un libellé en dur")
    void c36_erreursTraduites() throws Exception {
        api.send(adminToken(), put("/api/v1/club"), Map.of("name", ""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("club.name.required"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value("Le nom du club est obligatoire."));
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

        // user_account est global : un compte peut appartenir à plusieurs clubs (0029).
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
    @DisplayName("C39 — sans prestataire configuré, le métier passe quand même")
    void c39_connecteursAbstraits() {
        int avant = ((NoopMessagingProvider) messagerie).appelsRecus();

        String canal = messagerie.envoyer("+212600000000", "test.message", Map.of());
        String lien = paiement.lienDePaiement(Money.of(35000, "MAD"), "TEST-1");

        // Aucun canal, aucun lien : l'appelant le sait, et n'échoue pas pour autant.
        assertThat(canal).isNull();
        assertThat(lien).isNull();
        assertThat(((NoopMessagingProvider) messagerie).appelsRecus()).isEqualTo(avant + 1);
    }

    // ---------------------------------------------------------------- aides

    private String nomDuClub() {
        return jdbc.queryForObject("select name from club where id = ?", String.class, clubA);
    }

    private int comptable(String table) {
        Integer n = jdbc.queryForObject("select count(*) from " + table, Integer.class);
        return n == null ? 0 : n;
    }

    private int comptableOu(String table, String condition) {
        Integer n = jdbc.queryForObject(
                "select count(*) from " + table + " where " + condition, Integer.class);
        return n == null ? 0 : n;
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
        return auth.jetonDe(Fixtures.ADMIN_A_EMAIL);
    }
}
