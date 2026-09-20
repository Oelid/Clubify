package ma.clubify.platform.controller;

import ma.clubify.generated.api.AuthApi;
import ma.clubify.generated.model.CurrentUser;
import ma.clubify.generated.model.LoginRequest;
import ma.clubify.generated.model.LoginResponse;
import ma.clubify.generated.model.MfaVerifyRequest;
import ma.clubify.generated.model.RefreshRequest;
import ma.clubify.generated.model.TokenPair;
import ma.clubify.platform.model.dto.AuthOutcome;
import ma.clubify.platform.model.dto.Tokens;
import ma.clubify.platform.service.AuthService;
import ma.clubify.platform.service.UserQueryService;
import org.springframework.http.ResponseEntity;
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

    public AuthController(AuthService authentification, UserQueryService utilisateurs) {
        this.authentification = authentification;
        this.utilisateurs = utilisateurs;
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
        return ResponseEntity.ok(reponse);
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<TokenPair> verifyMfa(MfaVerifyRequest demande) {
        Tokens jetons = authentification.verifierSecondFacteur(
                demande.getMfaChallengeId(), demande.getCode(),
                Boolean.TRUE.equals(demande.getTrustDevice()), demande.getDeviceLabel());
        return ResponseEntity.ok(versContrat(jetons));
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<TokenPair> refreshToken(RefreshRequest demande) {
        return ResponseEntity.ok(versContrat(
                authentification.rafraichir(demande.getRefreshToken())));
    }

    @Override
    @PreAuthorize("@perm.authentifie()")
    public ResponseEntity<Void> logout() {
        authentification.deconnecter();
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@perm.authentifie()")
    public ResponseEntity<CurrentUser> getCurrentUser() {
        return ResponseEntity.ok(utilisateurs.utilisateurCourant());
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
