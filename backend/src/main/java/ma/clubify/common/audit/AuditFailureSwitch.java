package ma.clubify.common.audit;

import org.springframework.stereotype.Component;

/**
 * Permet de faire échouer volontairement l'écriture du journal, afin d'éprouver
 * que l'action échoue alors entièrement (critère C22b).
 *
 * <p>Activable uniquement depuis les tests : aucun point d'entrée de l'API ne
 * l'expose, et il reste inerte en exploitation.
 */
@Component
public class AuditFailureSwitch {

    private volatile boolean arme;

    public void armer() {
        arme = true;
    }

    public void desarmer() {
        arme = false;
    }

    void echouerSiDemande() {
        if (arme) {
            arme = false;
            throw new IllegalStateException("Écriture du journal volontairement mise en échec.");
        }
    }
}
