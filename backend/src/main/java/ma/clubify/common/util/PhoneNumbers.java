package ma.clubify.common.util;

import ma.clubify.common.exception.BusinessRuleException;

import java.util.regex.Pattern;

/**
 * Normalise un téléphone au format international E.164 (section 5).
 *
 * <p>Indicatif +212 par défaut : les numéros du club s'écrivent 06…, mais
 * WhatsApp et les SMS exigent la forme internationale (critère C37).
 */
public final class PhoneNumbers {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private static final String INDICATIF_PAR_DEFAUT = "+212";

    public static String normaliser(String saisie) {
        if (saisie == null || saisie.isBlank()) {
            return null;
        }

        String compact = saisie.replaceAll("[\\s.\\-()]", "");
        String candidat;
        if (compact.startsWith("+")) {
            candidat = compact;
        } else if (compact.startsWith("00")) {
            candidat = "+" + compact.substring(2);
        } else if (compact.startsWith("0")) {
            // 0612345678 devient +212612345678 : le zéro national disparaît.
            candidat = INDICATIF_PAR_DEFAUT + compact.substring(1);
        } else {
            candidat = INDICATIF_PAR_DEFAUT + compact;
        }

        if (!E164.matcher(candidat).matches()) {
            throw new BusinessRuleException("phone.invalid");
        }
        return candidat;
    }

    private PhoneNumbers() {
    }
}
