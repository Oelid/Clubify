package ma.clubify.common.audit;

/**
 * Règle automatique en cours d'exécution.
 *
 * <p>Sans cela, une action du système s'inscrirait au journal sans qu'on sache
 * laquelle. Le benchmark l'avait relevé chez iClassPro : les automatismes y sont
 * des auteurs à part entière (écart B1, critère C16b).
 */
public final class SystemActor {

    private static final ThreadLocal<String> REGLE = new ThreadLocal<>();

    /** Exécute une règle automatique en la nommant dans le journal. */
    public static void executer(String nomDeLaRegle, Runnable regle) {
        REGLE.set(nomDeLaRegle);
        try {
            regle.run();
        } finally {
            REGLE.remove();
        }
    }

    static String regleCourante() {
        return REGLE.get();
    }

    private SystemActor() {
    }
}
