package ma.clubify.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.web.method.HandlerTypePredicate;

/**
 * Préfixe commun de l'API.
 *
 * <p>Le contrat déclare {@code servers: /api/v1}, mais le générateur n'en tient
 * pas compte dans les chemins des interfaces. Le préfixe est donc posé ici, et
 * seulement sur les contrôleurs de l'application : Actuator reste à la racine.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    public static final String BASE_PATH = "/api/v1";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurateur) {
        configurateur.addPathPrefix(BASE_PATH,
                HandlerTypePredicate.forBasePackage("ma.clubify.platform.controller"));
    }
}
