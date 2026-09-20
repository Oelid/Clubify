package ma.clubify.platform.controller;

import ma.clubify.generated.api.ClubApi;
import ma.clubify.generated.model.Club;
import ma.clubify.generated.model.ClubUpdateRequest;
import ma.clubify.generated.model.SettingDefinition;
import ma.clubify.generated.model.SettingUpdate;
import ma.clubify.generated.model.SettingValue;
import ma.clubify.generated.model.UpdateClubLogoRequest;
import ma.clubify.platform.model.dto.ClubDto;
import ma.clubify.platform.service.ClubService;
import ma.clubify.platform.service.ClubSettingService;
import ma.clubify.platform.service.SettingDefinitions;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

/** Identité du club et registre de ses règles configurables. */
@RestController
public class ClubController implements ClubApi {

    private final ClubService club;

    public ClubController(ClubService club) {
        this.club = club;
    }

    @Override
    @PreAuthorize("@perm.a('club.settings.consulter')")
    public ResponseEntity<Club> getClub() {
        return ResponseEntity.ok(versContrat(club.lire()));
    }

    @Override
    @PreAuthorize("@perm.a('club.settings.modifier')")
    public ResponseEntity<Club> updateClub(ClubUpdateRequest demande) {
        return ResponseEntity.ok(versContrat(club.modifier(
                demande.getName(), demande.getLegalForm(), demande.getIce(), demande.getTaxId(),
                demande.getTradeRegister(), demande.getAddress(), demande.getPhone(),
                demande.getEmail(), demande.getTimezone(), demande.getCurrency(),
                demande.getDefaultLanguage())));
    }

    @Override
    @PreAuthorize("@perm.a('club.settings.modifier')")
    public ResponseEntity<Void> updateClubLogo(UpdateClubLogoRequest demande) {
        club.definirLogo(demande.getFileId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@perm.a('club.settings.consulter')")
    public ResponseEntity<List<SettingValue>> getClubSettings() {
        return ResponseEntity.ok(versContrat(club.reglages()));
    }

    @Override
    @PreAuthorize("@perm.a('club.settings.modifier')")
    public ResponseEntity<List<SettingValue>> updateClubSettings(List<SettingUpdate> demandes) {
        List<Map.Entry<String, Object>> souhaits = demandes.stream()
                .map(d -> (Map.Entry<String, Object>)
                        new AbstractMap.SimpleEntry<>(d.getKey(), d.getValue()))
                .toList();
        return ResponseEntity.ok(versContrat(club.definirReglages(souhaits)));
    }

    @Override
    @PreAuthorize("@perm.a('club.settings.consulter')")
    public ResponseEntity<List<SettingDefinition>> getSettingDefinitions() {
        List<SettingDefinition> definitions = SettingDefinitions.toutes().values().stream()
                .map(definition -> {
                    SettingDefinition contrat = new SettingDefinition();
                    contrat.setKey(definition.key());
                    contrat.setType(SettingDefinition.TypeEnum.fromValue(definition.type().name()));
                    contrat.setScope(SettingDefinition.ScopeEnum.fromValue(
                            definition.scope().name()));
                    contrat.setDefaultValue(definition.defaultValue());
                    contrat.setSource(definition.source());
                    return contrat;
                })
                .toList();
        return ResponseEntity.ok(definitions);
    }

    // ------------------------------------------------------- conversions

    private static Club versContrat(ClubDto club) {
        Club contrat = new Club();
        contrat.setId(club.id());
        contrat.setName(club.name());
        contrat.setTimezone(club.timezone());
        contrat.setCurrency(club.currency());
        contrat.setLogoFileId(club.logoFileId());
        contrat.setLegalForm(club.legalForm());
        contrat.setIce(club.ice());
        contrat.setTaxId(club.taxId());
        contrat.setTradeRegister(club.tradeRegister());
        contrat.setAddress(club.address());
        contrat.setPhone(club.phone());
        contrat.setEmail(club.email());
        contrat.setDefaultLanguage(club.defaultLanguage());
        return contrat;
    }

    private static List<SettingValue> versContrat(
            Map<String, ClubSettingService.ValeurEffective> reglages) {
        return reglages.entrySet().stream().map(entree -> {
            SettingValue valeur = new SettingValue();
            valeur.setKey(entree.getKey());
            valeur.setValue(entree.getValue().value());
            valeur.setOverridden(entree.getValue().overridden());
            return valeur;
        }).toList();
    }
}
