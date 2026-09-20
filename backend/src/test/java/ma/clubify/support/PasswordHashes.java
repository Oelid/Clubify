package ma.clubify.support;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Produit un haché Argon2 pour les jeux d'essai (décision 0029). */
public final class PasswordHashes {

    private static final PasswordEncoder ENCODER =
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    public static String of(String raw) {
        return ENCODER.encode(raw);
    }

    private PasswordHashes() {
    }
}
