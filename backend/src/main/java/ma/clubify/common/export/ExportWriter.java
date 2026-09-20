package ma.clubify.common.export;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Produit un export CSV ou Excel à partir d'une définition de colonnes.
 *
 * <p>Les colonnes marquées sensibles sont écartées ici, une fois pour toutes :
 * aucun appelant n'a à y penser, et aucun rôle ne peut les obtenir (SEC-03,
 * critère C34).
 */
@Component
public class ExportWriter {

    /** Séparateur point-virgule : Excel en français ne lit pas la virgule. */
    private static final char SEPARATEUR = ';';

    public <T> byte[] csv(List<Colonne<T>> colonnes, List<T> lignes) {
        List<Colonne<T>> retenues = retenues(colonnes);
        StringBuilder sortie = new StringBuilder();

        // Marque d'ordre des octets : sans elle, Excel affiche mal les accents.
        sortie.append('\uFEFF');
        joindre(sortie, retenues.stream().map(Colonne::entete).toList());

        for (T ligne : lignes) {
            joindre(sortie, retenues.stream()
                    .map(colonne -> texte(colonne.valeur().apply(ligne)))
                    .toList());
        }
        return sortie.toString().getBytes(StandardCharsets.UTF_8);
    }

    public <T> byte[] xlsx(String nomFeuille, List<Colonne<T>> colonnes, List<T> lignes) {
        List<Colonne<T>> retenues = retenues(colonnes);
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();

        try (Workbook classeur = new Workbook(sortie, "Clubify", "1.0")) {
            Worksheet feuille = classeur.newWorksheet(nomFeuille);
            for (int colonne = 0; colonne < retenues.size(); colonne++) {
                feuille.value(0, colonne, retenues.get(colonne).entete());
            }
            feuille.range(0, 0, 0, Math.max(0, retenues.size() - 1)).style().bold().set();

            for (int ligne = 0; ligne < lignes.size(); ligne++) {
                for (int colonne = 0; colonne < retenues.size(); colonne++) {
                    feuille.value(ligne + 1, colonne,
                            texte(retenues.get(colonne).valeur().apply(lignes.get(ligne))));
                }
            }
        } catch (IOException echec) {
            throw new UncheckedIOException("Écriture du classeur impossible.", echec);
        }
        return sortie.toByteArray();
    }

    private static <T> List<Colonne<T>> retenues(List<Colonne<T>> colonnes) {
        return colonnes.stream().filter(colonne -> !colonne.sensible()).toList();
    }

    private static void joindre(StringBuilder sortie, List<String> valeurs) {
        for (int i = 0; i < valeurs.size(); i++) {
            if (i > 0) {
                sortie.append(SEPARATEUR);
            }
            sortie.append(echapper(valeurs.get(i)));
        }
        sortie.append('\n');
    }

    private static String echapper(String valeur) {
        if (valeur.indexOf(SEPARATEUR) < 0 && valeur.indexOf('"') < 0 && valeur.indexOf('\n') < 0) {
            return valeur;
        }
        return '"' + valeur.replace("\"", "\"\"") + '"';
    }

    private static String texte(Object valeur) {
        return valeur == null ? "" : String.valueOf(valeur);
    }
}
