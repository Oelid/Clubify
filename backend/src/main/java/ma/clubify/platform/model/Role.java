package ma.clubify.platform.model;

/**
 * Les six rôles (SEC-02, décision 0028).
 *
 * <p>{@code ACCOUNT_ADMIN} est le titulaire du compte : tous les droits, non
 * retirables. À l'écran, « Administrateur du compte », à ne pas confondre avec
 * {@code FRONT_DESK}, l'accueil.
 */
public enum Role {
    ACCOUNT_ADMIN,
    MANAGER,
    FRONT_DESK,
    COACH,
    ACCOUNTANT,
    PARENT;

    /** Le second facteur est obligatoire pour ces rôles (décision 0027). */
    public boolean exigeSecondFacteur() {
        return this == ACCOUNT_ADMIN || this == MANAGER;
    }

    /** Aucun compte n'est encore attribuable à ces rôles en R1 (fiche F01, Q4). */
    public boolean attribuableEnR1() {
        return this != PARENT && this != ACCOUNTANT;
    }
}
