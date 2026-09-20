package ma.clubify.common.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Jeton de renouvellement en cookie {@code HttpOnly} (frontend/CLAUDE.md).
 *
 * <p>Un jeton lisible par du JavaScript est un jeton exfiltrable par la première
 * faille d'injection. Le navigateur ne voit donc jamais celui-ci : il l'envoie
 * sans le connaître. Le corps de la réponse le porte encore pour les clients
 * qui n'ont pas de cookies — tests, futures applications mobiles.
 *
 * <p>{@code SameSite=Strict} sert de protection CSRF : le cookie n'accompagne
 * aucune requête venue d'un autre site.
 */
@Component
public class RefreshCookie {

    public static final String NOM = "clubify_refresh";

    /** Le chemin borne l'envoi aux seuls points qui en ont besoin. */
    private static final String CHEMIN = "/api/v1/auth";

    private final boolean secure;
    private final Duration duree;

    public RefreshCookie(@Value("${clubify.auth.cookie-secure:true}") boolean secure,
                         @Value("${clubify.auth.refresh-ttl:P30D}") Duration duree) {
        this.secure = secure;
        this.duree = duree;
    }

    /** En-tête à poser sur la réponse pour installer le cookie. */
    public String poser(String jeton) {
        return construire(jeton, duree).toString();
    }

    /** En-tête à poser pour l'effacer, à la déconnexion. */
    public String effacer() {
        return construire("", Duration.ZERO).toString();
    }

    public String enTete() {
        return HttpHeaders.SET_COOKIE;
    }

    /** Le jeton du cookie, quand la demande en porte un. */
    public Optional<String> lire(HttpServletRequest demande) {
        Cookie[] cookies = demande.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> NOM.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(valeur -> valeur != null && !valeur.isBlank())
                .findFirst();
    }

    private ResponseCookie construire(String valeur, Duration age) {
        return ResponseCookie.from(NOM, valeur)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(CHEMIN)
                .maxAge(age)
                .build();
    }
}
