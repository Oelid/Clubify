package ma.clubify.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ma.clubify.common.security.Permissions;
import ma.clubify.platform.model.Role;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Rend exécutables les règles de {@code backend/CLAUDE.md}.
 * Le code généré depuis le contrat est exclu : il n'est pas écrit à la main.
 */
class ArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(location -> !location.contains("/generated/"))
            .importPackages("ma.clubify");

    @Test
    @DisplayName("Un contrôleur ne dépend jamais d'un repository")
    void controleurSansRepository() {
        ArchRule regle = noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..repository..");
        regle.check(CLASSES);
    }

    @Test
    @DisplayName("Un contrôleur ne dépend jamais d'une entité JPA")
    void controleurSansEntite() {
        ArchRule regle = noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..model.entity..");
        regle.check(CLASSES);
    }

    @Test
    @DisplayName("Un service ne dépend jamais d'un contrôleur")
    void serviceSansControleur() {
        ArchRule regle = noClasses()
                .that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAPackage("..controller..");
        regle.check(CLASSES);
    }

    @Test
    @DisplayName("Un domaine n'accède jamais au repository d'un autre domaine")
    void pasDeRepositoryCroise() {
        ArchRule regle = noClasses()
                .that().resideInAPackage("ma.clubify.platform..")
                .should().dependOnClassesThat().resideInAPackage("ma.clubify.family..repository..");
        regle.check(CLASSES);
    }

    /**
     * Critère C11b : aucun point d'entrée de l'API n'existe sans permission
     * déclarée (fiche F01, règle 11 ; décision 0028).
     */
    @Test
    @DisplayName("C11b — tout point d'entrée porte une permission déclarée")
    void toutPointDEntreePorteUnePermission() {
        ArchRule regle = methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .and().arePublic()
                .should().beAnnotatedWith(PreAuthorize.class)
                .because("décision 0028 : aucun point d'entrée sans permission déclarée ; "
                        + "les points d'entrée ouverts portent @PreAuthorize(\"permitAll()\")");
        regle.check(CLASSES);
    }

    /**
     * Un rôle ne reçoit jamais un droit qu'il ne peut pas exercer. Un droit
     * inopérant se réveille au premier ajustement : accorder la lecture d'une
     * liste ouvrirait alors son export sans que personne l'ait voulu.
     */
    @Test
    @DisplayName("Aucun rôle ne reçoit « exporter » sans « consulter »")
    void exporterSupposeConsulter() {
        for (Role role : Role.values()) {
            Set<String> defauts = Permissions.parDefaut(role);
            for (String permission : defauts) {
                if (!permission.endsWith(".exporter")) {
                    continue;
                }
                String lecture = permission.replace(".exporter", ".consulter");
                assertThat(defauts)
                        .as("%s détient %s : il lui faut %s", role, permission, lecture)
                        .contains(lecture);
            }
        }
    }

    @Test
    @DisplayName("Aucune entité n'utilise @Data, @EqualsAndHashCode ni @ToString")
    void lombokEncadreSurLesEntites() {
        ArchRule regle = noClasses()
                .that().resideInAPackage("..model.entity..")
                .should().beAnnotatedWith("lombok.Data")
                .orShould().beAnnotatedWith("lombok.EqualsAndHashCode")
                .orShould().beAnnotatedWith("lombok.ToString")
                .because("relations lazy et récursion (backend/CLAUDE.md)");
        regle.check(CLASSES);
    }

    @Test
    @DisplayName("Aucun service ne lit l'heure courante sans Clock")
    void horlogeInjectee() {
        ArchRule regle = noClasses()
                .that().resideInAPackage("..service..")
                .should().callMethod(java.time.Instant.class, "now")
                .because("l'heure vient d'un Clock injecté, pour que les règles soient testables");
        regle.check(CLASSES);
    }
}
