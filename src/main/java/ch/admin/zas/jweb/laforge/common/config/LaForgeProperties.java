package ch.admin.zas.jweb.laforge.common.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Racine des propriétés de configuration spécifiques à La Forge, liées depuis le préfixe
 * {@code laforge} (voir {@code application.yml}). Utilise des records imbriqués pour bénéficier
 * de la liaison par constructeur (immutabilité, validation au démarrage si une valeur manque).
 */
@ConfigurationProperties(prefix = "laforge")
public record LaForgeProperties(Security security, Mail mail, RateLimit rateLimit, Http http) {

    /**
     * Paramètres liés à l'authentification : émission des JWT, jetons de renouvellement et
     * jetons de vérification de compte.
     */
    public record Security(Jwt jwt, RefreshToken refreshToken, VerificationToken verificationToken) {

        /**
         * @param privateKeyLocation emplacement PEM de la clé privée RSA ; vide en développement,
         *                            une paire est alors générée en mémoire au démarrage.
         * @param publicKeyLocation  emplacement PEM de la clé publique RSA correspondante.
         */
        public record Jwt(
                String issuer,
                String audience,
                Duration accessTokenTtl,
                String privateKeyLocation,
                String publicKeyLocation) {
        }

        public record RefreshToken(Duration ttl) {
        }

        public record VerificationToken(Duration ttl, Duration resendCooldown) {
        }
    }

    public record Mail(String from, String verificationBaseUrl) {
    }

    /** Quotas de limitation de débit par famille d'opérations sensibles. */
    public record RateLimit(Bucket auth, Bucket tutor) {

        public record Bucket(int capacity, Duration refillPeriod) {
        }
    }

    /** Durcissement transverse des réponses et requêtes HTTP. */
    public record Http(DataSize maxRequestBodySize) {
    }
}
