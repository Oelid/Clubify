package ma.clubify.common.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import ma.clubify.config.AuthenticatedUser;
import ma.clubify.platform.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

/**
 * Jetons d'accès auto-émis et signés (décision 0024) : aucun serveur d'identité
 * à héberger ni à conformer (0018). Le jeton porte le club, le rôle et les
 * permissions effectives ; le client ne les fournit jamais (PLT-01).
 *
 * <p>La signature et l'analyse s'appuient sur Nimbus, fourni par Spring
 * Security : rien n'est écrit à la main sur un sujet où une erreur se paie
 * cher.
 */
@Service
public class TokenService {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

    private final SecureRandom alea = new SecureRandom();
    private final Clock horloge;
    private final byte[] secret;
    private final Duration dureeAcces;
    private final Duration dureeRafraichissement;

    public TokenService(
            Clock horloge,
            @Value("${clubify.security.jwt.secret:}") String secret,
            @Value("${clubify.security.access-token-minutes:15}") long accesMinutes,
            @Value("${clubify.security.refresh-token-days:30}") long rafraichissementJours) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "clubify.security.jwt.secret doit faire au moins 32 caractères : "
                            + "un secret plus court affaiblit la signature HS256.");
        }
        this.horloge = horloge;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.dureeAcces = Duration.ofMinutes(accesMinutes);
        this.dureeRafraichissement = Duration.ofDays(rafraichissementJours);
    }

    public Duration dureeAcces() {
        return dureeAcces;
    }

    public Duration dureeRafraichissement() {
        return dureeRafraichissement;
    }

    public String emettre(AuthenticatedUser utilisateur) {
        var maintenant = horloge.instant();
        JWTClaimsSet revendications = new JWTClaimsSet.Builder()
                .subject(utilisateur.userId().toString())
                .claim("club_id", utilisateur.clubId().toString())
                .claim("membership_id", utilisateur.membershipId().toString())
                .claim("role", utilisateur.role().name())
                .claim("perms", String.join(" ", utilisateur.permissions()))
                .claim("lang", utilisateur.language())
                .claim("mfa_pending", utilisateur.mfaPending())
                // « iat » est tronqué à la seconde ; la borne de révocation des
                // sessions se compare à la milliseconde (critères C8b, C9).
                .claim("iat_ms", maintenant.toEpochMilli())
                .issueTime(Date.from(maintenant))
                .expirationTime(Date.from(maintenant.plus(dureeAcces)))
                .build();

        try {
            SignedJWT jeton = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), revendications);
            jeton.sign(new MACSigner(secret));
            return jeton.serialize();
        } catch (JOSEException echec) {
            throw new IllegalStateException("Signature du jeton impossible.", echec);
        }
    }

    /** L'utilisateur porté par le jeton, ou {@code null} s'il est invalide ou expiré. */
    public AuthenticatedUser lire(String jeton) {
        try {
            SignedJWT signe = SignedJWT.parse(jeton);
            if (!signe.verify(new MACVerifier(secret))) {
                return null;
            }
            JWTClaimsSet revendications = signe.getJWTClaimsSet();
            Date expiration = revendications.getExpirationTime();
            if (expiration == null || expiration.toInstant().isBefore(horloge.instant())) {
                return null;
            }

            Number emissionPrecise = revendications.getLongClaim("iat_ms");
            Date emission = revendications.getIssueTime();
            String permissions = revendications.getStringClaim("perms");
            return new AuthenticatedUser(
                    UUID.fromString(revendications.getSubject()),
                    UUID.fromString(revendications.getStringClaim("club_id")),
                    UUID.fromString(revendications.getStringClaim("membership_id")),
                    Role.valueOf(revendications.getStringClaim("role")),
                    permissions == null || permissions.isBlank()
                            ? Set.of()
                            : Set.of(permissions.split(" ")),
                    revendications.getStringClaim("lang"),
                    Boolean.TRUE.equals(revendications.getBooleanClaim("mfa_pending")),
                    emissionPrecise != null
                            ? java.time.Instant.ofEpochMilli(emissionPrecise.longValue())
                            : emission == null ? null : emission.toInstant());
        } catch (Exception echec) {
            // Un jeton illisible est un jeton refusé : jamais une erreur serveur.
            return null;
        }
    }

    /** Jeton opaque de rafraîchissement : seule son empreinte est conservée. */
    public String nouveauJetonOpaque() {
        byte[] octets = new byte[32];
        alea.nextBytes(octets);
        return B64.encodeToString(octets);
    }

    public String empreinte(String jeton) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(jeton.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception echec) {
            throw new IllegalStateException("Empreinte impossible.", echec);
        }
    }
}
