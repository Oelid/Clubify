package ma.clubify.platform.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Taille des listes rendues par l'API.
 *
 * <p>Le club choisit combien de lignes il veut voir ; l'application refuse
 * d'aller au-delà de {@value #MAXIMUM}. Une page de mille lignes coûte autant au
 * serveur qu'au navigateur, et personne ne lit mille lignes : la borne protège
 * les deux, y compris d'un appel direct à l'API qui demanderait n'importe quoi.
 */
@Service
public class PaginationPolicy {

    /** Plafond absolu, qu'aucun réglage de club ne dépasse. */
    public static final int MAXIMUM = 100;

    /** Valeurs proposées à l'écran ; toute autre valeur reste acceptée. */
    public static final int[] PALIERS = {20, 30, 50, 100};

    private final ClubSettingService reglages;

    public PaginationPolicy(ClubSettingService reglages) {
        this.reglages = reglages;
    }

    /** Nombre de lignes par page pour ce club, borné. */
    public int tailleDuClub() {
        return borner((int) reglages.entier("ui.page_size"));
    }

    /**
     * Pagination à appliquer, à partir de ce que le client a demandé.
     *
     * <p>Une taille hors bornes est ramenée dans les bornes plutôt que refusée :
     * l'appelant obtient une page utilisable, et le serveur reste protégé.
     */
    public Pageable de(Integer page, Integer taille) {
        int demandee = taille == null ? tailleDuClub() : taille;
        return PageRequest.of(page == null || page < 0 ? 0 : page, borner(demandee));
    }

    private int borner(int taille) {
        return Math.min(Math.max(taille, 1), MAXIMUM);
    }
}
