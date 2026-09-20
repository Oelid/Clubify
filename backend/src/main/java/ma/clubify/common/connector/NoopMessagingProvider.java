package ma.clubify.common.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Connecteur de messagerie sans effet, retenu tant qu'aucun prestataire n'est
 * choisi (R3).
 *
 * <p>Il existe pour que le métier n'ait jamais à savoir s'il y a un prestataire
 * ou non : l'action aboutit dans les deux cas (critère C39).
 */
public class NoopMessagingProvider implements MessagingProvider {

    private static final Logger JOURNAL = LoggerFactory.getLogger(NoopMessagingProvider.class);

    private final AtomicInteger appels = new AtomicInteger();

    @Override
    public String envoyer(String destinataire, String modele, Map<String, String> variables) {
        appels.incrementAndGet();
        // Ni destinataire ni variables : elles porteraient des données de famille.
        JOURNAL.debug("Message {} non envoyé : aucun prestataire configuré.", modele);
        return null;
    }

    /** Nombre d'appels reçus, pour que les tests constatent le passage. */
    public int appelsRecus() {
        return appels.get();
    }
}
