package ma.clubify.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Règle générale de restitution des listes (décision 0033).
 *
 * <p>Toute liste rendue par l'API est paginée, et sa taille est bornée par le
 * serveur. Tenir cette règle par la seule discipline ne marche pas : la
 * prochaine feature qui ajoutera une liste l'oubliera, et l'oubli ne se verra
 * qu'en production, le jour où un club aura mille familles.
 *
 * <p>Ces contrôles la rendent impossible à oublier : ils décrivent la règle
 * aussi bien qu'ils la vérifient.
 */
class PaginationTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(location -> !location.contains("/generated/"))
            .importPackages("ma.clubify");

    private static final Path CONTRAT = Path.of("..", "contracts", "openapi.yaml");

    /** La politique de pagination : le seul endroit où la borne s'applique. */
    private static final String POLITIQUE = "ma.clubify.platform.service.PaginationPolicy";

    @Test
    @DisplayName("Aucun contrôleur ne construit sa pagination lui-même")
    void paginationJamaisFabriqueeAMain() {
        noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.data.domain.PageRequest")
                .because("la borne de cent lignes est posée par PaginationPolicy ; "
                        + "fabriquer une PageRequest à la main la contourne (décision 0033)")
                .check(CLASSES);
    }

    @Test
    @DisplayName("Tout point d'entrée qui rend une page passe par la politique de pagination")
    void toutePageBornee() {
        methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .and().arePublic()
                .and(new com.tngtech.archunit.base.DescribedPredicate<JavaMethod>(
                        "rendent une page") {
                    @Override
                    public boolean test(JavaMethod methode) {
                        return rendUnePage(methode);
                    }
                })
                .should(appelerLaPolitique())
                .because("une liste non bornée finit par rendre mille lignes (décision 0033)")
                .check(CLASSES);
    }

    /**
     * Le contrat déclare la pagination partout où il rend une page.
     *
     * <p>Le contrat est écrit avant le code : c'est là que l'oubli commence.
     */
    @Test
    @DisplayName("Le contrat déclare page et size sur toute opération qui rend une page")
    void contratDeclareLaPagination() throws IOException {
        assertThat(operationsSansPagination(nouveauLecteur()))
                .as("opérations rendant une page sans déclarer page et size")
                .isEmpty();
    }

    /**
     * Le contrôle ci-dessus repère bien une liste ajoutée sans pagination.
     *
     * <p>On ne peut pas le prouver en abîmant le vrai contrat : le générateur
     * produirait une méthode que le contrôleur n'implémente pas, et la
     * compilation s'arrêterait avant le test. Un contrat fabriqué pour
     * l'occasion le montre sans rien casser.
     */
    @Test
    @DisplayName("Ce contrôle repère une liste ajoutée sans pagination")
    void controleNonVide() {
        Map<String, Object> contratFabrique = new Yaml().load("""
                paths:
                  /familles:
                    get:
                      operationId: listFamilies
                      responses:
                        '200':
                          content:
                            application/json:
                              schema: { $ref: '#/components/schemas/FamilyPage' }
                  /familles-bien-faites:
                    get:
                      operationId: listFamiliesOk
                      parameters:
                        - $ref: '#/components/parameters/Page'
                        - $ref: '#/components/parameters/Size'
                      responses:
                        '200':
                          content:
                            application/json:
                              schema: { $ref: '#/components/schemas/FamilyPage' }
                """);

        assertThat(operationsSansPagination(contratFabrique))
                .containsExactly("GET /familles");
    }

    private static List<String> operationsSansPagination(Map<String, Object> contrat) {
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> chemins =
                (Map<String, Map<String, Object>>) contrat.get("paths");

        List<String> sansPagination = new ArrayList<>();
        chemins.forEach((chemin, operations) -> operations.forEach((verbe, brut) -> {
            if (!(brut instanceof Map<?, ?> operation) || !rendUnePage(operation)) {
                return;
            }
            Set<String> parametres = parametresDe(operation);
            if (!parametres.contains("Page") || !parametres.contains("Size")) {
                sansPagination.add(verbe.toUpperCase() + " " + chemin);
            }
        }));
        return sansPagination;
    }

    // ---------------------------------------------------------------- aides

    private static Map<String, Object> nouveauLecteur() throws IOException {
        try (var flux = Files.newInputStream(CONTRAT)) {
            return new Yaml().load(flux);
        }
    }

    /** Une réponse dont le schéma se nomme « …Page » rend une liste paginée. */
    private static boolean rendUnePage(Map<?, ?> operation) {
        return String.valueOf(operation.get("responses")).contains("Page'")
                || String.valueOf(operation.get("responses")).contains("Page}");
    }

    private static Set<String> parametresDe(Map<?, ?> operation) {
        Object parametres = operation.get("parameters");
        if (!(parametres instanceof List<?> liste)) {
            return Set.of();
        }
        return liste.stream()
                .map(String::valueOf)
                .map(texte -> texte.substring(texte.lastIndexOf('/') + 1).replace("}", ""))
                .collect(java.util.stream.Collectors.toSet());
    }

    private static boolean rendUnePage(JavaMethod methode) {
        // Le contrat nomme ses pages « …Page » : UserPage, AuditEntryPage.
        String rendu = methode.getRawReturnType().getName();
        return rendu.contains("ResponseEntity")
                ? methode.getReturnType().toString().contains("Page")
                : rendu.endsWith("Page");
    }

    private static ArchCondition<JavaMethod> appelerLaPolitique() {
        return new ArchCondition<>("passer par PaginationPolicy") {
            @Override
            public void check(JavaMethod methode, ConditionEvents evenements) {
                boolean passe = methode.getMethodCallsFromSelf().stream()
                        .anyMatch(appel -> appel.getTargetOwner().getName().equals(POLITIQUE));
                evenements.add(new SimpleConditionEvent(methode, passe,
                        methode.getFullName() + (passe ? " passe" : " ne passe pas")
                                + " par PaginationPolicy"));
            }
        };
    }
}
