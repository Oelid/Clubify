package ma.clubify.common.util;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Sérialise les états avant et après du journal d'audit.
 *
 * <p>Les colonnes sont en {@code jsonb} : un {@code Map.toString()} y produirait
 * du texte qui ressemble à du JSON sans en être, et la base le refuse.
 */
@Component
public class Json {

    private final ObjectMapper mapper;

    public Json(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String de(Object valeur) {
        return valeur == null ? null : mapper.writeValueAsString(valeur);
    }

    public String de(String cle, Object valeur) {
        return de(Map.of(cle, String.valueOf(valeur)));
    }
}
