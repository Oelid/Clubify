package ma.clubify.common.connector;

import ma.clubify.common.model.Money;

/**
 * Lien de paiement en ligne (INT-02, FAC-10).
 *
 * <p>Le club pilote n'a pas d'acquéreur (décision 0011) : l'interface existe
 * pour que le jour venu, brancher un prestataire ne touche pas au métier.
 */
public interface PaymentProvider {

    /** URL de paiement, ou {@code null} si aucun acquéreur n'est configuré. */
    String lienDePaiement(Money montant, String reference);
}
