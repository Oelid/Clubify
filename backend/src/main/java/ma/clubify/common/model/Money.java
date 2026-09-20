package ma.clubify.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Montant en centimes avec sa devise. Jamais de {@code BigDecimal} ni de
 * {@code double} : ni en base, ni dans le contrat (CLAUDE.md §3).
 */
@Embeddable
public record Money(
        @Column(name = "amount_cents") long amountCents,
        @Column(name = "currency", length = 3) String currency) {

    public static Money of(long amountCents, String currency) {
        return new Money(amountCents, currency);
    }

    public Money plus(Money autre) {
        exigerMemeDevise(autre);
        return new Money(amountCents + autre.amountCents, currency);
    }

    public Money minus(Money autre) {
        exigerMemeDevise(autre);
        return new Money(amountCents - autre.amountCents, currency);
    }

    public boolean isZero() {
        return amountCents == 0;
    }

    private void exigerMemeDevise(Money autre) {
        if (!currency.equals(autre.currency)) {
            throw new IllegalArgumentException(
                    "Devises différentes : " + currency + " et " + autre.currency);
        }
    }
}
