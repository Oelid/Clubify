package ma.clubify.config;

import ma.clubify.common.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


/**
 * Sécurité de l'API : sans session, jeton porteur, permissions vérifiées dans
 * la couche service et non seulement à l'écran (SEC-02, décision 0028).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter)
            throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(demandes -> demandes
                        // Pré-authentification : on ne peut pas exiger un jeton pour en obtenir un.
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/mfa/verify",
                                "/api/v1/auth/refresh").permitAll()
                        // Lien de fichier signé : le jeton du lien fait foi (PLT-05).
                        .requestMatchers(HttpMethod.GET, "/api/v1/files/download/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Argon2id par défaut, recommandation de l'OWASP (décision 0029). Le format
     * délégant laisse la porte ouverte à un changement d'algorithme sans
     * invalider les mots de passe existants.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        Argon2PasswordEncoder argon2 = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        return argon2;
    }
}
