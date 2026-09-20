package ma.clubify.platform.controller;

import jakarta.servlet.http.HttpServletRequest;
import ma.clubify.common.security.RefreshCookie;
import ma.clubify.generated.api.AuthApi;
import ma.clubify.generated.model.ClubSummary;
import ma.clubify.generated.model.CurrentUser;
import ma.clubify.generated.model.LoginRequest;
import ma.clubify.generated.model.LoginResponse;
import ma.clubify.generated.model.MfaVerifyRequest;
import ma.clubify.generated.model.RefreshRequest;
import ma.clubify.generated.model.TokenPair;
import ma.clubify.platform.model.dto.AuthOutcome;
import ma.clubify.platform.model.dto.CurrentUserDto;
import ma.clubify.platform.model.dto.Tokens;
import ma.clubify.platform.service.AuthService;
import ma.clubify.platform.service.UserQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentification. Le contrôleur ne décide de rien : il traduit la demande,
 * appelle le service et met en forme la réponse (backend/CLAUDE.md).
 */
@RestController
public class AuthController implements AuthApi {

    private final AuthService authentification;
    private final UserQueryService utilisateurs;
    private final RefreshCookie cookie;
    private final HttpServletRequest demandeCourante;

    public AuthController(AuthService authentification, UserQueryService utilisateurs,
                          RefreshCookie cookie, HttpServletRequest demandeCourante) {
        this.authentification = authentification;
        this.utilisateurs = utilisateurs;
        this.cookie = cookie;
        this.demandeCourante = demandeCourante;
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<LoginResponse> login(LoginRequest demande) {
        AuthOutcome issue = authentification.connecter(
                demande.getEmail(), demande.getPassword(), demande.getDeviceToken());

        LoginResponse reponse = new LoginResponse();
        reponse.setOutcome(LoginResponse.OutcomeEnum.fromValue(issue.kind().name()));
        if (issue.tokens() != null) {
            reponse.setTokens(versContrat(issue.tokens()));
        }
        if (issue.mfaChallengeId() != null) {
            reponse.setMfaChallengeId(issue.mfaChallengeId());
        }
        return avecCookie(issue.tokens()).body(reponse);
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<TokenPair> verifyMfa(MfaVerifyRequest demande) {
        Tokens jetons = authentification.verifierSecondFacteur(
                demande.getMfaChallengeId(), demande.getCode(),
                Boolean.TRUE.equals(demande.getTrustDevice()), demande.getDeviceLabel());
        return avecCookie(jetons).body(versContrat(jetons));
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<TokenPair> refreshToken(RefreshRequest demande) {
        String jeton = demande.getRefreshToken() != null && !demande.getRefreshToken().isBlank()
                ? demande.getRefreshToken()
                : cookie.lire(demandeCourante).orElse(null);

        Tokens jetons = authentification.rafraichir(jeton);
        return avecCookie(jetons).body(versContrat(jetons));
    }

    @Override
    @PreAuthorize("@perm.authentifie()")
    public ResponseEntity<Void> logout() {
        authentification.deconnecter();
        return ResponseEntity.noContent()
                .header(cookie.enTete(), cookie.effacer())
                .build();
    }

    @Override
    @PreAuthorize("@perm.authentifie()")
    public ResponseEntity<CurrentUser> getCurrentUser() {
        return ResponseEntity.ok(versContrat(utilisateurs.utilisateurCourant()));
    }

    /**
     * Pose le jeton de renouvellement en cookie {@code HttpOnly} : le navigateur
     * ne doit jamais pouvoir le lire (frontend/CLAUDE.md).
     */
    private ResponseEntity.BodyBuilder avecCookie(Tokens jetons) {
        ResponseEntity.BodyBuilder reponse = ResponseEntity.ok();
        if (jetons != null && jetons.refreshToken() != null) {
            reponse.header(cookie.enTete(), cookie.poser(jetons.refreshToken()));
        }
        return reponse;
    }

    private static CurrentUser versContrat(CurrentUserDto courant) {
        CurrentUser contrat = new CurrentUser();
        contrat.setId(courant.id());
        contrat.setEmail(courant.email());
        contrat.setFirstName(courant.firstName());
        contrat.setLastName(courant.lastName());
        contrat.setLanguage(courant.language());
        contrat.setRole(ma.clubify.generated.model.Role.fromValue(courant.role()));
        contrat.setMfaEnabled(courant.mfaEnabled());
        contrat.setPermissions(courant.permissions());

        ClubSummary club = new ClubSummary();
        club.setId(courant.club().id());
        club.setName(courant.club().name());
        club.setTimezone(courant.club().timezone());
        club.setCurrency(courant.club().currency());
        club.setLogoFileId(courant.club().logoFileId());
        club.setBrandPrimary(courant.club().brandPrimary());
        club.setBrandSecondary(courant.club().brandSecondary());
        contrat.setClub(club);
        return contrat;
    }

    private static TokenPair versContrat(Tokens jetons) {
        TokenPair paire = new TokenPair();
        paire.setAccessToken(jetons.accessToken());
        paire.setRefreshToken(jetons.refreshToken());
        paire.setExpiresInSeconds((int) jetons.expiresInSeconds());
        paire.setDeviceToken(jetons.deviceToken());
        return paire;
    }
}
