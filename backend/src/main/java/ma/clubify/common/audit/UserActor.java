package ma.clubify.common.audit;

import java.util.UUID;

/**
 * Auteur d'une action menée hors de toute demande authentifiée.
 *
 * <p>Une connexion en est le cas type : au moment où elle réussit, aucun jeton
 * n'est encore posé dans le contexte de sécurité. Sans cette portée, le journal
 * l'attribuerait au système, et la colonne « auteur » resterait vide là où elle
 * est le plus utile (SEC-04).
 */
public final class UserActor {

    private static final ThreadLocal<Auteur> AUTEUR = new ThreadLocal<>();

    /** Exécute une action en l'attribuant explicitement à cet utilisateur. */
    public static void executer(UUID userId, String libelle, Runnable action) {
        AUTEUR.set(new Auteur(userId, libelle));
        try {
            action.run();
        } finally {
            AUTEUR.remove();
        }
    }

    static Auteur courant() {
        return AUTEUR.get();
    }

    record Auteur(UUID userId, String libelle) {
    }

    private UserActor() {
    }
}
