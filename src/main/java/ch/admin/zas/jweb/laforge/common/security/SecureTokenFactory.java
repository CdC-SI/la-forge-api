package ch.admin.zas.jweb.laforge.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Génère des jetons opaques à haute entropie (vérification de compte, renouvellement de session)
 * et calcule leur empreinte SHA-256 pour un stockage sûr : seule l'empreinte est persistée, jamais
 * le jeton en clair.
 */
@Component
public class SecureTokenFactory {

    private static final int TOKEN_BYTES = 32; // 256 bits d'entropie, conforme au contrat (>=128 bits)

    private final SecureRandom secureRandom = new SecureRandom();

    /** Génère un jeton opaque encodé en Base64 URL, sans remplissage. */
    public String generateOpaqueToken() {
        var bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Empreinte SHA-256 hexadécimale d'un jeton en clair, destinée à la persistance. */
    public String hash(String rawToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 doit être disponible sur toute JVM standard.", e);
        }
    }
}
