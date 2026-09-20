package ma.clubify.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.clubify.config.AuthenticatedUser;
import ma.clubify.platform.repository.UserAccountRepository;
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
 *
 * <p>Le filtre vérifie aussi que le compte est toujours actif et que le jeton
 * n'a pas été révoqué : un jeton signé reste valable jusqu'à son échéance, ce
 * qui laisserait quinze minutes de sursis après une désactivation ou une
 * fermeture de sessions (critères C8b et C9).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXE = "Bearer ";

    private final TokenService jetons;
    private final TenantContext contexte;
    private final UserAccountRepository comptes;

    public JwtAuthenticationFilter(TokenService jetons, TenantContext contexte,
                                   UserAccountRepository comptes) {
        this.jetons = jetons;
        this.contexte = contexte;
        this.comptes = comptes;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest demande,
                                    @NonNull HttpServletResponse reponse,
                                    @NonNull FilterChain chaine)
            throws ServletException, IOException {
        try {
            AuthenticatedUser utilisateur = lireUtilisateur(demande);
            if (utilisateur != null && encoreValable(utilisateur)) {
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

    /**
     * Un jeton n'est valable que s'il a été émis après la dernière fermeture des
     * sessions du compte (critères C8b, C9).
     *
     * <p>Aucune tolérance : l'horodatage d'émission d'un JWT est tronqué à la
     * seconde, si bien qu'un jeton émis dans la même seconde qu'une fermeture
     * paraît antérieur à elle. Il est alors refusé, et l'utilisateur se
     * reconnecte. L'inverse — laisser passer une seconde de jetons après une
     * fermeture demandée — serait une porte ouverte sur un compte que le gérant
     * croit fermé.
     */
    private boolean encoreValable(AuthenticatedUser utilisateur) {
        return comptes.findById(utilisateur.userId())
                .filter(compte -> compte.isActive())
                .filter(compte -> utilisateur.issuedAt() == null
                        || !utilisateur.issuedAt().isBefore(compte.getSessionsValidFrom()))
                .isPresent();
    }

    private AuthenticatedUser lireUtilisateur(HttpServletRequest demande) {
        String entete = demande.getHeader("Authorization");
        if (entete == null || !entete.startsWith(PREFIXE)) {
            return null;
        }
        return jetons.lire(entete.substring(PREFIXE.length()));
    }
}
