package ch.admin.zas.jweb.laforge.security.config;

import ch.admin.zas.jweb.laforge.common.config.LaForgeProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Fournit la paire de clés RSA utilisée pour signer et valider les JWT d'accès, ainsi que les
 * beans {@link JwtEncoder}/{@link JwtDecoder} associés. En l'absence de clés configurées
 * (propriétés {@code laforge.security.jwt.*-key-location}), une paire est générée en mémoire au
 * démarrage : pratique en développement, inadapté à un déploiement à plusieurs instances (les
 * jetons émis par une instance ne seraient pas valides sur une autre).
 */
@Configuration
public class JwtKeysConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtKeysConfig.class);

    @Bean
    public KeyPair jwtSigningKeyPair(LaForgeProperties properties) throws IOException {
        var jwtProperties = properties.security().jwt();
        var hasConfiguredKeys = isNotBlank(jwtProperties.privateKeyLocation())
                && isNotBlank(jwtProperties.publicKeyLocation());
        if (hasConfiguredKeys) {
            return loadKeyPair(jwtProperties.privateKeyLocation(), jwtProperties.publicKeyLocation());
        }
        log.warn("Aucune clé RSA configurée pour la signature JWT : génération d'une paire en mémoire. "
                + "À réserver au développement ; configurer laforge.security.jwt.private-key-location "
                + "et public-key-location en production.");
        return generateEphemeralKeyPair();
    }

    @Bean
    public JwtEncoder jwtEncoder(KeyPair jwtSigningKeyPair) {
        var publicKey = (RSAPublicKey) jwtSigningKeyPair.getPublic();
        var privateKey = (RSAPrivateKey) jwtSigningKeyPair.getPrivate();
        var jwk = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        var jwkSource = new ImmutableJWKSet<SecurityContext>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(KeyPair jwtSigningKeyPair) {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) jwtSigningKeyPair.getPublic()).build();
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private KeyPair generateEphemeralKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("L'algorithme RSA doit être disponible sur toute JVM standard.", e);
        }
    }

    private KeyPair loadKeyPair(String privateKeyLocation, String publicKeyLocation) throws IOException {
        try {
            var keyFactory = KeyFactory.getInstance("RSA");
            var privateKeyBytes = readPemBytes(privateKeyLocation);
            var publicKeyBytes = readPemBytes(publicKeyLocation);
            var privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            var publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            return new KeyPair(publicKey, privateKey);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Impossible de charger la paire de clés RSA configurée.", e);
        }
    }

    private byte[] readPemBytes(String location) throws IOException {
        var resource = new DefaultResourceLoader().getResource(location);
        try (var input = resource.getInputStream()) {
            var content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            var normalized = content
                    .replaceAll("-----BEGIN [^-]+-----", "")
                    .replaceAll("-----END [^-]+-----", "")
                    .replaceAll("\\s", "");
            return Base64.getDecoder().decode(normalized);
        }
    }
}
