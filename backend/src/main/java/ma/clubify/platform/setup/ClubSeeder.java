package ma.clubify.platform.setup;

import ma.clubify.common.audit.SystemActor;
import ma.clubify.common.event.DomainEvent;
import ma.clubify.common.event.DomainEvents;
import ma.clubify.common.model.entity.UuidV7;
import ma.clubify.common.security.TenantContext;
import ma.clubify.platform.model.Role;
import ma.clubify.platform.model.entity.Club;
import ma.clubify.platform.model.entity.Membership;
import ma.clubify.platform.model.entity.Site;
import ma.clubify.platform.model.entity.UserAccount;
import ma.clubify.platform.repository.ClubRepository;
import ma.clubify.platform.repository.MembershipRepository;
import ma.clubify.platform.repository.SiteRepository;
import ma.clubify.platform.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Amorçage d'un club et de son premier administrateur (fiche F01, Q1).
 *
 * <p>Sans lui, une base neuve n'a aucun compte : personne ne peut se connecter,
 * donc personne ne peut créer de compte. C'est le seul chemin d'écriture qui
 * n'exige pas de permission, et il n'existe qu'en développement et en
 * démonstration : le profil de production ne le charge pas.
 *
 * <p>Usage : {@code --seed-club --club="Nom du club" --admin=adresse@exemple.test
 * --password=…}. Sans mot de passe fourni, un mot de passe est tiré au hasard et
 * affiché une fois — aucun mot de passe par défaut n'existe.
 */
@Component
@Profile({"dev", "demo"})
public class ClubSeeder implements ApplicationRunner {

    private static final Logger journal = LoggerFactory.getLogger(ClubSeeder.class);

    private static final String DECLENCHEUR = "seed-club";
    private static final String CLUB_PAR_DEFAUT = "Club de démonstration";
    private static final String ADMIN_PAR_DEFAUT = "admin@exemple.test";

    private final ClubRepository clubs;
    private final SiteRepository sites;
    private final UserAccountRepository comptes;
    private final MembershipRepository appartenances;
    private final PasswordEncoder motsDePasse;
    private final DomainEvents evenements;
    private final TenantContext contexte;
    private final TransactionTemplate transaction;
    private final Clock horloge;

    public ClubSeeder(ClubRepository clubs, SiteRepository sites, UserAccountRepository comptes,
                      MembershipRepository appartenances, PasswordEncoder motsDePasse,
                      DomainEvents evenements, TenantContext contexte,
                      TransactionTemplate transaction, Clock horloge) {
        this.clubs = clubs;
        this.sites = sites;
        this.comptes = comptes;
        this.appartenances = appartenances;
        this.motsDePasse = motsDePasse;
        this.evenements = evenements;
        this.contexte = contexte;
        this.transaction = transaction;
        this.horloge = horloge;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!arguments.containsOption(DECLENCHEUR)) {
            return;
        }
        // Auteur « système », règle nommée : la création du premier compte est
        // une action sensible, elle laisse une trace comme les autres (règle 17).
        String nomDuClub = option(arguments, "club", CLUB_PAR_DEFAUT);
        String email = option(arguments, "admin", ADMIN_PAR_DEFAUT);
        String motDePasse = option(arguments, "password", null);

        if (comptes.existsByEmailIgnoreCase(email)) {
            journal.warn("Amorçage ignoré : un compte existe déjà pour cette adresse.");
            return;
        }

        // Le club se nomme avant d'être écrit : Hibernate résout le discriminant à
        // l'ouverture de la session, donc le poser dans la transaction serait trop
        // tard (PLT-01).
        UUID clubId = UuidV7.next();
        contexte.set(clubId);
        try {
            // La transaction s'ouvre ici, explicitement : un @Transactional sur une
            // méthode appelée depuis la même classe ne passe pas par le proxy, et
            // l'audit, qui l'exige, échouerait — laissant un club créé sans trace.
            SystemActor.executer("setup.seed-club", () -> transaction.executeWithoutResult(
                    statut -> amorcer(arguments, clubId, nomDuClub, email, motDePasse)));
        } finally {
            contexte.clear();
        }
    }

    private void amorcer(ApplicationArguments arguments, UUID clubId, String nomDuClub,
                         String email, String motDePasseFourni) {

        boolean motDePasseGenere = motDePasseFourni == null;
        String motDePasse = motDePasseGenere
                ? MotsDePasseAleatoires.tirer()
                : motDePasseFourni;

        Club club = new Club();
        club.setId(clubId);
        club.setName(nomDuClub);
        club.setTimezone("Africa/Casablanca");
        club.setCurrency("MAD");
        club.setDefaultLanguage("fr");
        clubs.save(club);

        Site site = new Site();
        site.setId(UuidV7.next());
        site.setClubId(club.getId());
        site.setName(nomDuClub);
        sites.save(site);

        UserAccount admin = new UserAccount();
        admin.setId(UuidV7.next());
        admin.setEmail(email);
        // Nom volontairement neutre : le club le remplace par celui de la
        // personne. « Administrateur du compte » est le libellé du rôle, pas un nom.
        admin.setFirstName(option(arguments, "first-name", "Compte"));
        admin.setLastName(option(arguments, "last-name", "À renommer"));
        admin.setLanguage("fr");
        admin.setPasswordHash(motsDePasse.encode(motDePasse));
        admin.setActive(true);
        admin.setSessionsValidFrom(horloge.instant());
        comptes.save(admin);

        Membership appartenance = new Membership();
        appartenance.setId(UuidV7.next());
        appartenance.setClubId(club.getId());
        appartenance.setUserId(admin.getId());
        appartenance.setRole(Role.ACCOUNT_ADMIN);
        appartenance.setActive(true);
        appartenances.save(appartenance);

        evenements.publish(DomainEvent.of(club.getId(), "club.created", "Club", club.getId()));
        evenements.publish(DomainEvent.of(club.getId(), "user.created", "UserAccount",
                admin.getId()));

        // Seule trace du mot de passe : cette ligne, en développement, une fois.
        journal.info("Club « {} » amorcé. Administrateur : {}", nomDuClub, email);
        if (motDePasseGenere) {
            journal.info("Mot de passe tiré au hasard, à noter maintenant : {}", motDePasse);
        }
        journal.info("Le second facteur sera exigé à la première connexion (décision 0027).");
    }

    private static String option(ApplicationArguments arguments, String nom, String defaut) {
        List<String> valeurs = arguments.getOptionValues(nom);
        return valeurs == null || valeurs.isEmpty() ? defaut : valeurs.getFirst();
    }

    /** Mot de passe d'amorçage : jamais une valeur fixe, jamais dans le dépôt. */
    private static final class MotsDePasseAleatoires {
        private static String tirer() {
            return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
    }
}
