package ma.clubify.platform.controller;

import ma.clubify.common.security.RefreshCookie;
import ma.clubify.generated.api.ProfileApi;
import ma.clubify.generated.model.MfaConfirmRequest;
import ma.clubify.generated.model.MfaSetupResponse;
import ma.clubify.generated.model.RecoveryCodes;
import ma.clubify.generated.model.TokenPair;
import ma.clubify.generated.model.TrustedDevice;
import ma.clubify.platform.model.dto.Tokens;
import ma.clubify.platform.model.dto.TrustedDeviceDto;
import ma.clubify.platform.service.MfaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Actions de l'utilisateur sur son propre compte.
 *
 * <p>Ces points restent ouverts à un jeton dont le second facteur est encore à
 * activer : c'est justement là que l'activation se fait (critère C6b).
 */
@RestController
public class ProfileController implements ProfileApi {

    private final MfaService secondFacteur;
    private final RefreshCookie cookie;

    public ProfileController(MfaService secondFacteur, RefreshCookie cookie) {
        this.secondFacteur = secondFacteur;
        this.cookie = cookie;
    }

    @Override
    @PreAuthorize("@perm.authentifie()")
    public ResponseEntity<MfaSetupResponse> setupMfa() {
        MfaService.Preparation preparation = secondFacteur.preparer();

        MfaSetupResponse reponse = new MfaSetupResponse();
        reponse.setOtpauthUri(preparation.otpauthUri());
        reponse.setRecoveryCodes(preparation.recoveryCodes());
        return ResponseEntity.ok(reponse);
    }

    @Override
    @PreAuthorize("@perm.authentifie()")
    public ResponseEntity<TokenPair> confirmMfa(MfaConfirmRequest demande) {
        Tokens jetons = secondFacteur.confirmer(demande.getCode());

        TokenPair paire = new TokenPair();
        paire.setAccessToken(jetons.accessToken());
        paire.setRefreshToken(jetons.refreshToken());
        paire.setExpiresInSeconds((int) jetons.expiresInSeconds());
        return ResponseEntity.ok()
                .header(cookie.enTete(), cookie.poser(jetons.refreshToken()))
                .body(paire);
    }

    @Override
    @PreAuthorize("@perm.authentifie()")
    public ResponseEntity<RecoveryCodes> regenerateRecoveryCodes() {
        RecoveryCodes reponse = new RecoveryCodes();
        reponse.setCodes(secondFacteur.regenererCodesDeSecours());
        return ResponseEntity.ok(reponse);
    }

    @Override
    @PreAuthorize("@perm.authentifie()")
    public ResponseEntity<List<TrustedDevice>> listTrustedDevices() {
        List<TrustedDevice> appareils = secondFacteur.appareilsDeConfiance().stream()
                .map(ProfileController::versContrat)
                .toList();
        return ResponseEntity.ok(appareils);
    }

    @Override
    @PreAuthorize("@perm.authentifie()")
    public ResponseEntity<Void> revokeTrustedDevice(UUID deviceId) {
        secondFacteur.revoquerAppareil(deviceId);
        return ResponseEntity.noContent().build();
    }

    private static TrustedDevice versContrat(TrustedDeviceDto appareil) {
        TrustedDevice contrat = new TrustedDevice();
        contrat.setId(appareil.id());
        contrat.setLabel(appareil.label());
        contrat.setExpiresAt(appareil.expiresAt().atOffset(java.time.ZoneOffset.UTC));
        if (appareil.lastUsedAt() != null) {
            contrat.setLastUsedAt(appareil.lastUsedAt().atOffset(java.time.ZoneOffset.UTC));
        }
        return contrat;
    }
}
