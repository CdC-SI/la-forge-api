package ch.admin.zas.jweb.laforge.common.page;

import ch.admin.zas.jweb.laforge.common.error.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Encode et décode les curseurs opaques de pagination. Un curseur porte l'empreinte des filtres
 * ayant produit la page précédente ainsi que les valeurs de clé de tri du dernier élément lu
 * (pagination par clés, stable face aux insertions concurrentes). Un curseur réutilisé avec des
 * filtres différents est rejeté (400), conformément au contrat.
 */
public final class CursorCodec {

    private CursorCodec() {
    }

    /**
     * Calcule une empreinte stable des filtres de la requête courante (clés triées, valeurs nulles
     * ignorées) afin de détecter la réutilisation d'un curseur avec d'autres filtres.
     */
    public static String fingerprint(Map<String, ?> filters) {
        var canonical = filters.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        return HexFormat.of().formatHex(digest(canonical), 0, 8);
    }

    /** Encode l'empreinte des filtres et les valeurs de clé de tri du dernier élément de la page. */
    public static String encode(String filterFingerprint, List<String> keyValues) {
        var payload = filterFingerprint + ":" + String.join(",", keyValues);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Décode le curseur et vérifie qu'il correspond bien aux filtres courants.
     *
     * @throws BadRequestException si le curseur est malformé ou a été émis avec d'autres filtres
     */
    public static List<String> decode(String cursor, String expectedFingerprint) {
        try {
            var payload = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            var separatorIndex = payload.indexOf(':');
            if (separatorIndex < 0) {
                throw malformedCursor();
            }
            var fingerprint = payload.substring(0, separatorIndex);
            if (!fingerprint.equals(expectedFingerprint)) {
                throw new BadRequestException(
                        "Le curseur doit être réutilisé avec les mêmes filtres que la page précédente.");
            }
            var rest = payload.substring(separatorIndex + 1);
            return rest.isEmpty() ? List.of() : List.of(rest.split(",", -1));
        } catch (IllegalArgumentException e) {
            throw malformedCursor();
        }
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 doit être disponible sur toute JVM standard.", e);
        }
    }

    private static BadRequestException malformedCursor() {
        return new BadRequestException("Curseur invalide ou expiré.");
    }
}
