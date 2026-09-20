package ma.clubify.platform.service;

import ma.clubify.common.model.entity.UuidV7;
import ma.clubify.common.security.PermissionChecker;
import ma.clubify.platform.model.entity.ClubSetting;
import ma.clubify.platform.repository.ClubSettingRepository;
import ma.clubify.platform.service.SettingDefinitions.Definition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Lit et écrit les règles configurables du club (section 9.8).
 *
 * <p>Une règle non saisie prend son défaut : aucune valeur n'est jamais copiée
 * en base « pour initialiser », ce qui permet de faire évoluer un défaut sans
 * migration (critère C31).
 */
@Service
public class ClubSettingService {

    private final ClubSettingRepository valeurs;
    private final ObjectMapper json;

    public ClubSettingService(ClubSettingRepository valeurs, ObjectMapper json) {
        this.valeurs = valeurs;
        this.json = json;
    }

    /** Valeur effective : celle du club si elle existe, sinon le défaut. */
    public Object valeur(String cle) {
        Definition definition = SettingDefinitions.definition(cle);
        return valeurs.findBySettingKey(cle)
                .map(saisie -> lire(saisie.getValue()))
                .orElse(definition.defaultValue());
    }

    public long entier(String cle) {
        return ((Number) valeur(cle)).longValue();
    }

    public String texte(String cle) {
        Object valeur = valeur(cle);
        return valeur == null ? null : String.valueOf(valeur);
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> liste(String cle) {
        return (java.util.List<String>) valeur(cle);
    }

    /** Toutes les valeurs effectives, avec l'indication de leur origine. */
    public Map<String, ValeurEffective> toutes() {
        Map<String, String> saisies = new HashMap<>();
        valeurs.findAll().forEach(v -> saisies.put(v.getSettingKey(), v.getValue()));

        Map<String, ValeurEffective> effectives = new java.util.LinkedHashMap<>();
        SettingDefinitions.toutes().forEach((cle, definition) -> {
            String saisie = saisies.get(cle);
            effectives.put(cle, saisie == null
                    ? new ValeurEffective(definition.defaultValue(), false)
                    : new ValeurEffective(lire(saisie), true));
        });
        return effectives;
    }

    @Transactional
    public void definir(String cle, Object valeur) {
        Definition definition = SettingDefinitions.definition(cle);
        if (definition.scope() == SettingDefinitions.Scope.PLATFORM) {
            throw new ma.clubify.common.exception.BusinessRuleException("setting.notClubScoped");
        }

        ClubSetting saisie = valeurs.findBySettingKey(cle).orElseGet(() -> {
            ClubSetting nouvelle = new ClubSetting();
            nouvelle.setId(UuidV7.next());
            nouvelle.setClubId(PermissionChecker.requis().clubId());
            nouvelle.setSettingKey(cle);
            return nouvelle;
        });
        saisie.setValue(json.writeValueAsString(valeur));
        valeurs.save(saisie);
    }

    private Object lire(String brut) {
        return json.readValue(brut, Object.class);
    }

    /** Une valeur effective et son origine : défaut du registre, ou saisie du club. */
    public record ValeurEffective(Object value, boolean overridden) {
    }
}
