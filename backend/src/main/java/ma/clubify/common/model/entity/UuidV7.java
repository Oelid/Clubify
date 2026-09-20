package ma.clubify.common.model.entity;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Identifiants UUID version 7 : aléatoires, mais ordonnés dans le temps, ce qui
 * évite la fragmentation des index sans exposer de compteur (backend/CLAUDE.md).
 */
public final class UuidV7 {

    private static final SecureRandom ALEA = new SecureRandom();

    public static UUID next() {
        return from(Instant.now());
    }

    static UUID from(Instant instant) {
        byte[] octets = new byte[16];
        ALEA.nextBytes(octets);

        long millis = instant.toEpochMilli();
        octets[0] = (byte) (millis >>> 40);
        octets[1] = (byte) (millis >>> 32);
        octets[2] = (byte) (millis >>> 24);
        octets[3] = (byte) (millis >>> 16);
        octets[4] = (byte) (millis >>> 8);
        octets[5] = (byte) millis;

        octets[6] = (byte) ((octets[6] & 0x0F) | 0x70);   // version 7
        octets[8] = (byte) ((octets[8] & 0x3F) | 0x80);   // variante RFC 4122

        long fort = 0;
        long faible = 0;
        for (int i = 0; i < 8; i++) {
            fort = (fort << 8) | (octets[i] & 0xFF);
        }
        for (int i = 8; i < 16; i++) {
            faible = (faible << 8) | (octets[i] & 0xFF);
        }
        return new UUID(fort, faible);
    }

    private UuidV7() {
    }
}
