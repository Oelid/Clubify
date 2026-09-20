package ma.clubify.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Chiffrement des champs et des fichiers sensibles : santé, CIN, pièces, secret
 * du second facteur (SEC-03, décision 0029).
 *
 * <p>AES-256-GCM. La clé vient de l'environnement, jamais du dépôt ; son
 * identifiant est stocké à côté de la donnée pour permettre une rotation sans
 * réécrire l'existant.
 */
@Service
public class EncryptionService {

    private static final String ALGORITHME = "AES/GCM/NoPadding";
    private static final int TAILLE_NONCE = 12;
    private static final int TAILLE_MARQUE_BITS = 128;

    private final SecureRandom alea = new SecureRandom();
    private final SecretKey cle;
    private final String identifiantDeCle;

    public EncryptionService(
            @Value("${clubify.encryption.key:}") String cleBase64,
            @Value("${clubify.encryption.key-id:k1}") String identifiantDeCle) {
        if (cleBase64 == null || cleBase64.isBlank()) {
            throw new IllegalStateException(
                    "clubify.encryption.key est absente : aucune donnée sensible ne peut être "
                            + "protégée. Elle se fournit par variable d'environnement (SEC-03).");
        }
        byte[] octets = Base64.getDecoder().decode(cleBase64);
        if (octets.length != 32) {
            throw new IllegalStateException(
                    "clubify.encryption.key doit faire 32 octets une fois décodée (AES-256).");
        }
        this.cle = new SecretKeySpec(octets, "AES");
        this.identifiantDeCle = identifiantDeCle;
    }

    public String identifiantDeCle() {
        return identifiantDeCle;
    }

    public byte[] chiffrer(byte[] clair) {
        try {
            byte[] nonce = new byte[TAILLE_NONCE];
            alea.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(ALGORITHME);
            cipher.init(Cipher.ENCRYPT_MODE, cle, new GCMParameterSpec(TAILLE_MARQUE_BITS, nonce));
            byte[] chiffre = cipher.doFinal(clair);

            byte[] resultat = new byte[nonce.length + chiffre.length];
            System.arraycopy(nonce, 0, resultat, 0, nonce.length);
            System.arraycopy(chiffre, 0, resultat, nonce.length, chiffre.length);
            return resultat;
        } catch (Exception echec) {
            // Le message ne contient jamais la donnée en cause.
            throw new IllegalStateException("Chiffrement impossible.", echec);
        }
    }

    public byte[] dechiffrer(byte[] chiffre) {
        try {
            byte[] nonce = new byte[TAILLE_NONCE];
            System.arraycopy(chiffre, 0, nonce, 0, TAILLE_NONCE);

            Cipher cipher = Cipher.getInstance(ALGORITHME);
            cipher.init(Cipher.DECRYPT_MODE, cle, new GCMParameterSpec(TAILLE_MARQUE_BITS, nonce));
            return cipher.doFinal(chiffre, TAILLE_NONCE, chiffre.length - TAILLE_NONCE);
        } catch (Exception echec) {
            throw new IllegalStateException("Déchiffrement impossible.", echec);
        }
    }

    public String chiffrerTexte(String clair) {
        return Base64.getEncoder().encodeToString(chiffrer(clair.getBytes(StandardCharsets.UTF_8)));
    }

    public String dechiffrerTexte(String chiffreBase64) {
        return new String(dechiffrer(Base64.getDecoder().decode(chiffreBase64)), StandardCharsets.UTF_8);
    }
}
