package ma.clubify.common.connector;

/**
 * Envoi de messages aux parents : WhatsApp, SMS, courriel (INT-01).
 *
 * <p>Interface unique, prestataires interchangeables : aucun n'est codé en dur
 * (CLAUDE.md §3). Un canal en échec ne bloque jamais le métier — c'est pourquoi
 * les envois passent par l'outbox.
 */
public interface MessagingProvider {

    /** Canal réellement emprunté, ou {@code null} si aucun n'est configuré. */
    String envoyer(String destinataire, String modele, java.util.Map<String, String> variables);
}
