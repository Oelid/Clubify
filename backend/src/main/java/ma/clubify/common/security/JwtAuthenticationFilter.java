package ma.clubify.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.clubify.config.AuthenticatedUser;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Pose le contexte de la demande depuis le jeton : qui agit, et dans quel club.
 *
 * <p>C'est ici, et nulle part ailleurs, que le club est déterminé. Un
 * identifiant de club envoyé par le client est ignoré (PLT-01, critère C2).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXE = "Bearer ";

    private final TokenService jetons;
    private final TenantContext contexte;

    public JwtAuthenticationFilter(TokenService jetons, TenantContext contexte) {
        this.jetons = jetons;
        this.contexte = contexte;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest demande,
                                    @NonNull HttpServletResponse reponse,
                                    @NonNull FilterChain chaine)
            throws ServletException, IOException {
        try {
            AuthenticatedUser utilisateur = lireUtilisateur(demande);
            if (utilisateur != null) {
                contexte.set(utilisateur.clubId());
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(utilisateur, null, List.of()));
            }
            chaine.doFilter(demande, reponse);
        } finally {
            // Le fil d'exécution est mutualisé : ne rien laisser derrière soi.
            contexte.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private AuthenticatedUser lireUtilisateur(HttpServletRequest demande) {
        String entete = demande.getHeader("Authorization");
        if (entete == null || !entete.startsWith(PREFIXE)) {
            return null;
        }
        return jetons.lire(entete.substring(PREFIXE.length()));
    }
}
