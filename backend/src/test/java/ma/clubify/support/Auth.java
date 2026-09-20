package ma.clubify.support;

import ma.clubify.common.security.TotpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Connexion du staff dans les tests, par le parcours réel de l'API.
 *
 * <p>L'administrateur et le gérant doivent activer leur second facteur avant
 * d'accéder à quoi que ce soit (décision 0027) : cette aide franchit cette
 * étape, plutôt que de la contourner en base.
 */
@Component
public class Auth {

    private static final Pattern SECRET = Pattern.compile("secret=([^&]+)");

    @Autowired
    private Api api;

    @Autowired
    private TotpService totp;

    /** Jeton pleinement utilisable, second facteur activé si le rôle l'exige. */
    public String jetonDe(String email) throws Exception {
        String corps = api.loginRaw(email, Fixtures.VALID_PASSWORD)
                .andReturn().getResponse().getContentAsString();
        String issue = api.json().readTree(corps).path("outcome").asString();

        if ("AUTHENTICATED".equals(issue)) {
            return api.json().readTree(corps).path("tokens").path("accessToken").asString();
        }
        if ("MFA_ENROLLMENT_REQUIRED".equals(issue)) {
            return activer(email).jeton();
        }
        throw new IllegalStateException(
                "Connexion inattendue pour " + email + " : " + issue);
    }

    /**
     * Active le second facteur par le parcours réel : le secret vient de l'URI
     * otpauth que l'API retourne, et le code s'en calcule.
     */
    public Activation activer(String email) throws Exception {
        String corps = api.loginRaw(email, Fixtures.VALID_PASSWORD)
                .andReturn().getResponse().getContentAsString();
        String provisoire = api.json().readTree(corps).path("tokens").path("accessToken").asString();

        String preparation = api.send(provisoire, post("/api/v1/profile/mfa/setup"), null)
                .andReturn().getResponse().getContentAsString();
        var noeud = api.json().readTree(preparation);

        String secret = secretDe(noeud.path("otpauthUri").asString());
        List<String> codesDeSecours = new ArrayList<>();
        noeud.path("recoveryCodes").forEach(code -> codesDeSecours.add(code.asString()));

        api.send(provisoire, post("/api/v1/profile/mfa/confirm"),
                Map.of("code", totp.codeCourant(secret)));

        // Une fois activé, une vraie connexion passe par le défi.
        String defi = defiPour(email);
        String finale = api.send(null, post("/api/v1/auth/mfa/verify"),
                        Map.of("mfaChallengeId", defi, "code", totp.codeCourant(secret)))
                .andReturn().getResponse().getContentAsString();
        String jeton = api.json().readTree(finale).path("accessToken").asString();

        return new Activation(secret, codesDeSecours, jeton);
    }

    public String defiPour(String email) throws Exception {
        String corps = api.loginRaw(email, Fixtures.VALID_PASSWORD)
                .andReturn().getResponse().getContentAsString();
        return api.json().readTree(corps).path("mfaChallengeId").asString();
    }

    public String codeCourant(String secret) {
        return totp.codeCourant(secret);
    }

    private static String secretDe(String otpauthUri) {
        Matcher trouve = SECRET.matcher(otpauthUri);
        assertThat(trouve.find()).as("secret dans %s", otpauthUri).isTrue();
        return trouve.group(1);
    }

    /** Ce qu'une activation de second facteur laisse entre les mains du test. */
    public record Activation(String secret, List<String> codesDeSecours, String jeton) {
    }
}
