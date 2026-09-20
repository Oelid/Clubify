package ma.clubify.platform.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registre des règles configurables par club (section 9.8, fiche F01 règle 31).
 *
 * <p>Le défaut vit ici, en code, versionné avec la feature qui introduit la
 * règle ; la base ne porte que ce qu'un club a saisi. Chaque feature suivante
 * ajoutera les siennes, avec leur valeur par défaut documentée.
 */
public final class SettingDefinitions {

    /** Portée d'une règle : réglable par le club, ou fixée par Clubify. */
    public enum Scope {
        CLUB, PLATFORM
    }

    public enum Type {
        STRING, INTEGER, BOOLEAN, DECIMAL, JSON
    }

    public record Definition(String key, Type type, Scope scope, Object defaultValue,
                             String source) {
    }

    private static final Map<String, Definition> REGISTRE = new LinkedHashMap<>();

    private static void declarer(String cle, Type type, Scope portee, Object defaut, String source) {
        REGISTRE.put(cle, new Definition(cle, type, portee, defaut, source));
    }

    static {
        declarer("club.timezone", Type.STRING, Scope.CLUB, "Africa/Casablanca", "F01 règle 29");
        declarer("club.currency", Type.STRING, Scope.CLUB, "MAD", "F01 règle 29");
        declarer("club.default_language", Type.STRING, Scope.CLUB, "fr", "F01 règle 29 / PLT-08");
        // Palette par défaut : bleu acier et orange du logo. La marque du club
        // reste un accent, jamais la charte du produit ; un club qui a la sienne
        // remplace ces deux valeurs depuis l'écran des paramètres (décision 0032).
        declarer("club.brand.primary", Type.STRING, Scope.CLUB, "#307890", "0032");
        declarer("club.brand.secondary", Type.STRING, Scope.CLUB, "#F08840", "0032");

        declarer("security.mfa.trusted_device_days", Type.INTEGER, Scope.CLUB, 30,
                "F01 règle 5 / benchmark B4");
        // Le second facteur est attendu des rôles sensibles et rappelé en
        // permanence, sans bloquer : la première configuration d'un club ne
        // dépend pas d'un téléphone sous la main (décision 0031). Un club qui
        // veut l'imposer pose « required » ; il devient alors bloquant après le
        // délai de grâce.
        declarer("security.mfa.required", Type.BOOLEAN, Scope.CLUB, false, "0031");
        declarer("security.mfa.grace_days", Type.INTEGER, Scope.CLUB, 7, "0031");
        declarer("security.password.min_length", Type.INTEGER, Scope.PLATFORM, 12, "F01 règle 7");
        declarer("security.lockout.max_attempts", Type.INTEGER, Scope.PLATFORM, 5, "F01 règle 9");
        declarer("security.lockout.minutes", Type.INTEGER, Scope.PLATFORM, 15, "F01 règle 9");

        declarer("files.max_size_mb", Type.INTEGER, Scope.CLUB, 10, "F01 règle 24");
        declarer("files.allowed_types", Type.JSON, Scope.CLUB,
                java.util.List.of("application/pdf", "image/jpeg", "image/png"), "F01 règle 24");
        declarer("files.link_ttl_minutes", Type.INTEGER, Scope.CLUB, 15, "F01 règle 23");

        // Portées par F01, consommées par F08 et F09.
        declarer("billing.invoice_prefix", Type.STRING, Scope.CLUB, "F", "F01 règle 30 / FAC-01");
        declarer("billing.receipt_prefix", Type.STRING, Scope.CLUB, "R", "F01 règle 30 / FAC-07");
        declarer("billing.fiscal_year_start_month", Type.INTEGER, Scope.CLUB, 1,
                "F01 règle 30 ; exercice comptable à confirmer");
    }

    /**
      * Le registre, dans l'ordre où les règles sont déclarées.
      *
      * <p>Cet ordre groupe les règles par domaine — club, sécurité, fichiers,
      * facturation — et c'est celui que l'écran des paramètres présente. Une
      * copie non ordonnée y ferait voisiner la devise et la durée d'un lien.
      */
    public static Map<String, Definition> toutes() {
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(REGISTRE));
    }

    public static Definition definition(String cle) {
        Definition definition = REGISTRE.get(cle);
        if (definition == null) {
            throw new IllegalArgumentException("Règle inconnue du registre : " + cle);
        }
        return definition;
    }

    private SettingDefinitions() {
    }
}
