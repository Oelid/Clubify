package ma.clubify.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test d'intégration sur une vraie base PostgreSQL, jamais H2
 * (backend/CLAUDE.md, « Stratégie de tests »).
 *
 * <p>Les tests de F01 s'écrivent au niveau HTTP, contre le contrat de
 * {@code contracts/openapi.yaml}. Ils échouent tant que les contrôleurs
 * n'existent pas et passeront sans être réécrits.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Tag("integration")
public @interface IntegrationTest {
}
