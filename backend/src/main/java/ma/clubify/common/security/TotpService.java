package ma.clubify.common.security;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;

/**
 * Second facteur par code temporaire (TOTP, RFC 6238), implémenté avec le JDK :
 * aucune dépendance, aucun prestataire, et il fonctionne sans réseau sur le
 * téléphone de l'utilisateur (décision 0027).
 */
@Service
public class TotpService {

    private static final String ALGORITHME = "HmacSHA1";
    private static final int CHIFFRES = 6;
    private static final Duration PAS = Duration.ofSeconds(30);
    /** Tolérance d'un pas de part et d'autre : les horloges dérivent. */
    private static final int FENETRE = 1;
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final SecureRandom alea = new SecureRandom();
    private final Clock horloge;

    public TotpService(Clock horloge) {
        this.horloge = horloge;
    }

    /** Secret partagé, en base32, tel que l'attendent les applications. */
    public String nouveauSecret() {
        byte[] octets = new byte[20];
        alea.nextBytes(octets);
        return encoderBase32(octets);
    }

    /** URI {@code otpauth} à rendre en QR côté interface. */
    public String otpauthUri(String secret, String email, String emetteur) {
        return "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d"
                .formatted(emetteur, email, secret, emetteur, CHIFFRES, PAS.toSeconds());
    }

    public boolean verifier(String secret, String code) {
        if (code == null || code.length() != CHIFFRES) {
            return false;
        }
        long pasCourant = horloge.instant().getEpochSecond() / PAS.toSeconds();
        for (int decalage = -FENETRE; decalage <= FENETRE; decalage++) {
            if (code.equals(calculer(secret, pasCourant + decalage))) {
                return true;
            }
        }
        return false;
    }

    /** Le code attendu à cet instant ; sert aussi aux tests. */
    public String codeCourant(String secret) {
        return calculer(secret, horloge.instant().getEpochSecond() / PAS.toSeconds());
    }

    private String calculer(String secret, long pas) {
        try {
            byte[] cle = decoderBase32(secret);
            byte[] compteur = ByteBuffer.allocate(8).putLong(pas).array();

            Mac mac = Mac.getInstance(ALGORITHME);
            mac.init(new SecretKeySpec(cle, ALGORITHME));
            byte[] empreinte = mac.doFinal(compteur);

            int decalage = empreinte[empreinte.length - 1] & 0x0F;
            int binaire = ((empreinte[decalage] & 0x7F) << 24)
                    | ((empreinte[decalage + 1] & 0xFF) << 16)
                    | ((empreinte[decalage + 2] & 0xFF) << 8)
                    | (empreinte[decalage + 3] & 0xFF);

            return String.format("%0" + CHIFFRES + "d", binaire % (int) Math.pow(10, CHIFFRES));
        } catch (Exception echec) {
            throw new IllegalStateException("Calcul du code temporaire impossible.", echec);
        }
    }

    private static String encoderBase32(byte[] octets) {
        StringBuilder sortie = new StringBuilder();
        int tampon = 0;
        int bits = 0;
        for (byte octet : octets) {
            tampon = (tampon << 8) | (octet & 0xFF);
            bits += 8;
            while (bits >= 5) {
                sortie.append(BASE32.charAt((tampon >> (bits - 5)) & 0x1F));
                bits -= 5;
            }
        }
        if (bits > 0) {
            sortie.append(BASE32.charAt((tampon << (5 - bits)) & 0x1F));
        }
        return sortie.toString();
    }

    private static byte[] decoderBase32(String texte) {
        String propre = texte.replace("=", "").toUpperCase();
        byte[] sortie = new byte[propre.length() * 5 / 8];
        int tampon = 0;
        int bits = 0;
        int position = 0;
        for (char caractere : propre.toCharArray()) {
            tampon = (tampon << 5) | BASE32.indexOf(caractere);
            bits += 5;
            if (bits >= 8) {
                sortie[position++] = (byte) ((tampon >> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return sortie;
    }
}
