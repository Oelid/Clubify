package ma.clubify.support;

/**
 * Jeux d'essai. Aucun nom réel, aucune donnée issue des captures
 * (CLAUDE.md §7) : deux clubs fictifs et des personnes inventées.
 */
public final class Fixtures {

    public static final String CLUB_A = "Club A";
    public static final String CLUB_B = "Club B";

    public static final String ADMIN_A_EMAIL = "admin.a@example.test";
    public static final String MANAGER_A_EMAIL = "gerant.a@example.test";
    public static final String FRONT_DESK_A_EMAIL = "accueil.a@example.test";
    public static final String COACH_A_EMAIL = "coach.a@example.test";
    public static final String ADMIN_B_EMAIL = "admin.b@example.test";

    /** 12 caractères : la longueur minimale retenue (fiche F01, règle 7). */
    public static final String VALID_PASSWORD = "Motdepasse12";
    /** 11 caractères : un de moins que le minimum. */
    public static final String TOO_SHORT_PASSWORD = "Motdepasse1";

    private Fixtures() {
    }
}
