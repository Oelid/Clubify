package ma.clubify.platform;

import ma.clubify.common.audit.AuditFailureSwitch;
import ma.clubify.common.event.DomainEvents;
import ma.clubify.common.security.TenantContext;
import ma.clubify.platform.repository.ClubRepository;
import ma.clubify.platform.repository.MembershipRepository;
import ma.clubify.platform.repository.SiteRepository;
import ma.clubify.platform.repository.UserAccountRepository;
import ma.clubify.platform.setup.ClubSeeder;
import ma.clubify.support.Fixtures;
import ma.clubify.support.IntegrationTest;
import ma.clubify.support.TestSeeder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Amorçage d'un club (fiche F01, Q1).
 *
 * <p>Le composant ne vit qu'en profils {@code dev} et {@code demo} : il est
 * construit à la main plutôt que d'élargir sa condition de chargement pour la
 * commodité d'un test. Ses dépendances, elles, sont celles de l'application.
 *
 * <p>Ce test ferme une anomalie trouvée en base : les écritures se faisaient
 * hors transaction et se validaient une à une, laissant un club créé sans la
 * moindre trace d'audit — ce que la règle 17 interdit.
 */
@IntegrationTest
@DisplayName("Amorçage d'un club")
class ClubSeederTest {

    private static final String ADRESSE = "amorcage@exemple.test";
    private static final String MOT_DE_PASSE = "MotDePasseAmorcage2027";

    @Autowired
    private ClubRepository clubs;
    @Autowired
    private SiteRepository sites;
    @Autowired
    private UserAccountRepository comptes;
    @Autowired
    private MembershipRepository appartenances;
    @Autowired
    private PasswordEncoder motsDePasse;
    @Autowired
    private DomainEvents evenements;
    @Autowired
    private TenantContext contexte;
    @Autowired
    private TransactionTemplate transaction;
    @Autowired
    private Clock horloge;
    @Autowired
    private TestSeeder seeder;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private AuditFailureSwitch interrupteur;

    private ClubSeeder amorceur;

    @BeforeEach
    void preparer() {
        seeder.reset();
        amorceur = new ClubSeeder(clubs, sites, comptes, appartenances, motsDePasse,
                evenements, contexte, transaction, horloge);
    }

    @AfterEach
    void desarmer() {
        interrupteur.desarmer();
        contexte.clear();
    }

    @Test
    @DisplayName("C42 — le club, son site et son administrateur naissent ensemble, avec leur trace")
    void c42_amorcageComplet() {
        amorceur.run(arguments());

        assertThat(comptable("club")).isEqualTo(1);
        assertThat(comptable("site")).as("le club naît avec son site par défaut").isEqualTo(1);
        assertThat(comptable("user_account")).isEqualTo(1);

        String role = jdbc.queryForObject("select role from membership", String.class);
        assertThat(role).isEqualTo("ACCOUNT_ADMIN");

        // Le mot de passe n'est jamais stocké en clair (critère C5).
        String empreinte = jdbc.queryForObject(
                "select password_hash from user_account", String.class);
        assertThat(empreinte).startsWith("$argon2").doesNotContain(MOT_DE_PASSE);

        // Créer le premier compte d'un club est une action sensible : elle laisse
        // sa trace, au nom de la règle qui l'a menée (règle 17).
        assertThat(actions()).containsExactlyInAnyOrder("club.created", "user.created");
        assertThat(jdbc.queryForObject(
                "select distinct actor_label from audit_log", String.class))
                .isEqualTo("setup.seed-club");
    }

    @Test
    @DisplayName("C42b — si l'audit échoue, aucun club n'est créé")
    void c42b_toutOuRien() {
        interrupteur.armer();

        assertThatThrownBy(() -> amorceur.run(arguments()))
                .as("l'amorçage remonte l'échec plutôt que de laisser un club à moitié créé")
                .isInstanceOf(RuntimeException.class);

        // C'est l'anomalie trouvée en base : les écritures se validaient une à une,
        // et un club existait sans qu'aucune trace ne dise qui l'avait créé.
        assertThat(comptable("club")).isZero();
        assertThat(comptable("site")).isZero();
        assertThat(comptable("user_account")).isZero();
        assertThat(comptable("membership")).isZero();
    }

    @Test
    @DisplayName("C42c — relancer l'amorçage ne crée pas un second club")
    void c42c_relanceSansEffet() {
        amorceur.run(arguments());
        amorceur.run(arguments());

        // La commande tourne à chaque démarrage de l'application : elle doit
        // pouvoir passer sans rien faire.
        assertThat(comptable("club")).isEqualTo(1);
        assertThat(comptable("user_account")).isEqualTo(1);
    }

    @Test
    @DisplayName("C42d — sans l'option, l'amorçage ne touche à rien")
    void c42d_sansDeclencheur() {
        amorceur.run(new DefaultApplicationArguments());

        assertThat(comptable("club")).isZero();
    }

    // ---------------------------------------------------------------- aides

    private DefaultApplicationArguments arguments() {
        return new DefaultApplicationArguments(
                "--seed-club",
                "--club=" + Fixtures.CLUB_A,
                "--admin=" + ADRESSE,
                "--password=" + MOT_DE_PASSE);
    }

    private int comptable(String table) {
        Integer nombre = jdbc.queryForObject("select count(*) from " + table, Integer.class);
        return nombre == null ? 0 : nombre;
    }

    private java.util.List<String> actions() {
        return jdbc.queryForList("select action from audit_log", String.class);
    }
}
