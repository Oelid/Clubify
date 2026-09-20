package ma.clubify.common.connector;

import ma.clubify.common.model.Money;

/** Aucun acquéreur configuré : le lien n'existe pas, et le métier le sait. */
public class NoopPaymentProvider implements PaymentProvider {

    @Override
    public String lienDePaiement(Money montant, String reference) {
        return null;
    }
}
