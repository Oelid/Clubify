package ma.clubify.platform.service;

import ma.clubify.platform.model.Role;
import ma.clubify.platform.model.entity.UserAccount;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Quand le second facteur devient obligatoire (décision 0031).
 *
 * <p>Par défaut, il est <strong>attendu sans être imposé</strong> : l'interface
 * le rappelle en permanence à qui ne l'a pas activé, et rien ne bloque. La
 * première configuration d'un club ne dépend donc pas d'un téléphone sous la
 * main, et une règle qui empêcherait d'installer le logiciel ne sera pas
 * contournée.
 *
 * <p>Un club qui veut l'imposer pose {@code security.mfa.required} : le second
 * facteur devient alors bloquant après {@code security.mfa.grace_days}, et l'on
 * retrouve la règle de 0027. Le délai court depuis la création du compte, et non
 * depuis sa première connexion : un compte créé puis laissé de côté ne gagne pas
 * de sursis en restant inutilisé.
 */
@Service
public class MfaPolicy {

    private final ClubSettingService reglages;
    private final Clock horloge;

    public MfaPolicy(ClubSettingService reglages, Clock horloge) {
        this.reglages = reglages;
        this.horloge = horloge;
    }

    /** Où en est ce compte, pour ce rôle. */
    public Etat etatDe(UserAccount compte, Role role) {
        // Le rappel ne dépend pas de l'obligation : le rôle ouvre la caisse et
        // les données de santé, on le dit, que le club impose ou non.
        boolean attendu = role.exigeSecondFacteur();
        if (compte.isMfaEnabled() || !attendu) {
            return new Etat(compte.isMfaEnabled(), attendu && !compte.isMfaEnabled(), false, null);
        }
        if (!impose()) {
            return new Etat(false, true, false, null);
        }

        Instant echeance = echeanceDe(compte);
        return new Etat(false, true, !horloge.instant().isBefore(echeance), echeance);
    }

    /** Faut-il refuser l'accès à ce compte tant qu'il n'a pas activé ? */
    public boolean bloque(UserAccount compte, Role role) {
        return etatDe(compte, role).bloquant();
    }

    private boolean impose() {
        return Boolean.TRUE.equals(reglages.valeur("security.mfa.required"));
    }

    private Instant echeanceDe(UserAccount compte) {
        Instant depart = compte.getCreatedAt() == null ? horloge.instant() : compte.getCreatedAt();
        return depart.plus(Duration.ofDays(reglages.entier("security.mfa.grace_days")));
    }

    /**
     * @param actif    le second facteur est activé sur ce compte
     * @param attendu  le rôle l'attend et le compte ne l'a pas : il faut le rappeler
     * @param bloquant le délai est écoulé : plus rien n'est accessible sans lui
     * @param exigeA   date à laquelle il le deviendra ; nulle si le club ne l'impose pas
     */
    public record Etat(boolean actif, boolean attendu, boolean bloquant, Instant exigeA) {

        /** Vrai quand l'interface doit inviter à l'activer. */
        public boolean aRappeler() {
            return attendu && !actif;
        }

        /** Vrai quand le rappel doit annoncer une échéance. */
        public boolean aUneEcheance() {
            return exigeA != null;
        }
    }
}
