package ma.clubify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Renseigne « créé par » et « modifié par » depuis l'utilisateur authentifié.
 * Ces colonnes sont un filet, pas le journal d'audit : celui-ci porte l'avant et
 * l'après (SEC-04).
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfiguration {

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return () -> {
            Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
            if (authentification == null || !authentification.isAuthenticated()) {
                return Optional.empty();
            }
            if (authentification.getPrincipal() instanceof AuthenticatedUser utilisateur) {
                return Optional.of(utilisateur.userId());
            }
            return Optional.empty();
        };
    }
}
