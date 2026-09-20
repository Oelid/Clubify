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

    /** Secrets des comptes déjà activés, pour répondre aux défis suivants. */
    private final Map<String, String> secrets = new java.util.concurrent.ConcurrentHashMap<>();

    /** Jeton pleinement utilisable, second facteur activé si le rôle l'exige. */
    public String jetonDe(String email) throws Exception {
        String corps = api.loginRaw(email, Fixtures.VALID_PASSWORD)
                .andReturn().getResponse().getContentAsString();
        String issue = api.json().readTree(corps).path("outcome").asString();

        if ("AUTHENTICATED".equals(issue)) {
            String jeton = api.json().readTree(corps).path("tokens").path("accessToken").asString();
            // Depuis la décision 0031, la connexion aboutit pendant le délai de
            // grâce. Un test qui demande un jeton veut une session complète :
            // on active le second facteur quand le rôle l'attend encore.
            return secondFacteurAttendu(jeton) ? activer(email).jeton() : jeton;
        }
        if ("MFA_ENROLLMENT_REQUIRED".equals(issue)) {
            return activer(email).jeton();
        }
        if ("MFA_REQUIRED".equals(issue)) {
            String secret = secrets.get(email);
            assertThat(secret).as("second facteur déjà activé pour %s", email).isNotNull();
            String defi = api.json().readTree(corps).path("mfaChallengeId").asString();
            String finale = api.send(null, post("/api/v1/auth/mfa/verify"),
                            Map.of("mfaChallengeId", defi, "code", totp.codeCourant(secret)))
                    .andReturn().getResponse().getContentAsString();
            return api.json().readTree(finale).path("accessToken").asString();
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

        String secret = secretDansUri(noeud.path("otpauthUri").asString());
        List<String> codesDeSecours = new ArrayList<>();
        noeud.path("recoveryCodes").forEach(code -> codesDeSecours.add(code.asString()));

        // Confirmer le code ouvre la session : le mot de passe n'est pas redemandé.
        String activation = api.send(provisoire, post("/api/v1/profile/mfa/confirm"),
                        Map.of("code", totp.codeCourant(secret)))
                .andReturn().getResponse().getContentAsString();
        String jeton = api.json().readTree(activation).path("accessToken").asString();

        secrets.put(email, secret);
        return new Activation(secret, codesDeSecours, jeton);
    }

    /** Le rôle attend-il un second facteur que ce compte n'a pas encore ? */
    private boolean secondFacteurAttendu(String jeton) throws Exception {
        var mfa = api.json().readTree(api.getAs(jeton, "/auth/me")
                .andReturn().getResponse().getContentAsString()).path("mfa");
        return mfa.path("expected").asBoolean(false) && !mfa.path("enabled").asBoolean(false);
    }

    public String defiPour(String email) throws Exception {
        String corps = api.loginRaw(email, Fixtures.VALID_PASSWORD)
                .andReturn().getResponse().getContentAsString();
        return api.json().readTree(corps).path("mfaChallengeId").asString();
    }

    public String codeCourant(String secret) {
        return totp.codeCourant(secret);
    }

    /** Secret d'un compte déjà activé par cette aide, pour répondre à un défi. */
    public String secretDe(String email) {
        String secret = secrets.get(email);
        assertThat(secret).as("second facteur déjà activé pour %s", email).isNotNull();
        return secret;
    }

    private static String secretDansUri(String otpauthUri) {
        Matcher trouve = SECRET.matcher(otpauthUri);
        assertThat(trouve.find()).as("secret dans %s", otpauthUri).isTrue();
        return trouve.group(1);
    }

    /** Ce qu'une activation de second facteur laisse entre les mains du test. */
    public record Activation(String secret, List<String> codesDeSecours, String jeton) {
    }
}
