package ma.clubify.config;

import ma.clubify.common.connector.MessagingProvider;
import ma.clubify.common.connector.NoopMessagingProvider;
import ma.clubify.common.connector.NoopPaymentProvider;
import ma.clubify.common.connector.PaymentProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Connecteurs de messagerie et de paiement (INT-01, INT-02).
 *
 * <p>Aucun prestataire n'est codé en dur (CLAUDE.md §3) : tant qu'aucun n'est
 * configuré, une implémentation sans effet prend la place, et le métier aboutit
 * sans savoir s'il y en a un (critère C39). Brancher un prestataire consistera
 * à déclarer un bean, sans toucher aux appelants.
 */
@Configuration
public class ConnectorConfiguration {

    @Bean
    @ConditionalOnMissingBean(MessagingProvider.class)
    public MessagingProvider messagingProvider() {
        return new NoopMessagingProvider();
    }

    @Bean
    @ConditionalOnMissingBean(PaymentProvider.class)
    public PaymentProvider paymentProvider() {
        return new NoopPaymentProvider();
    }
}
