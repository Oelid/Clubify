package ma.clubify.common.export;

import java.util.function.Function;

/**
 * Colonne d'un export.
 *
 * @param entete   libellé traduit, jamais écrit en dur dans le code appelant
 * @param sensible vrai pour les données de SEC-03 — santé, CIN, pièces — qui ne
 *                 figurent dans aucun export, quel que soit le rôle (critère C34)
 */
public record Colonne<T>(String cle, String entete, Function<T, Object> valeur, boolean sensible) {

    public static <T> Colonne<T> de(String cle, String entete, Function<T, Object> valeur) {
        return new Colonne<>(cle, entete, valeur, false);
    }

    public static <T> Colonne<T> sensible(String cle, String entete, Function<T, Object> valeur) {
        return new Colonne<>(cle, entete, valeur, true);
    }
}
