package ma.clubify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * L'heure courante s'injecte, elle ne se lit jamais en dur : sans cela, aucune
 * règle datée n'est testable (backend/CLAUDE.md).
 */
@Configuration
public class ClockConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
