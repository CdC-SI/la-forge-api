package ch.admin.zas.jweb.laforge.common.idempotency;

import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Trace la déduplication d'une requête mutative portant un en-tête {@code Idempotency-Key}. La
 * clé n'est unique que combinée au compte, à la méthode HTTP et au chemin (contrat : « unique pour
 * utilisateur + méthode + chemin »), ce qui autorise un même client à réutiliser un UUID de clé
 * sur des routes différentes. La réponse complétée est conservée en JSON brut pour être rejouée
 * telle quelle, jusqu'à expiration ({@code expiresAt}, 24 h après complétion).
 */
@Entity
@Table(
        name = "idempotency_record",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_idempotency_record_scope",
                        columnNames = {"idempotency_key", "account_id", "http_method", "path"}))
public class IdempotencyRecord extends BaseEntity {

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private UUID idempotencyKey;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "http_method", nullable = false, updatable = false, length = 10)
    private String httpMethod;

    @Column(name = "path", nullable = false, updatable = false, length = 500)
    private String path;

    @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IdempotencyStatus status;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    protected IdempotencyRecord() {
    }

    public static IdempotencyRecord beginning(
            UUID idempotencyKey,
            UUID accountId,
            String httpMethod,
            String path,
            String requestHash,
            OffsetDateTime expiresAt) {
        var record = new IdempotencyRecord();
        record.idempotencyKey = idempotencyKey;
        record.accountId = accountId;
        record.httpMethod = httpMethod;
        record.path = path;
        record.requestHash = requestHash;
        record.status = IdempotencyStatus.IN_PROGRESS;
        record.expiresAt = expiresAt;
        return record;
    }

    /** Enregistre la réponse d'origine et prolonge la fenêtre de rejeu de 24 h à partir de maintenant. */
    public void complete(int responseStatus, String responseBody, OffsetDateTime expiresAt) {
        this.status = IdempotencyStatus.COMPLETED;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.expiresAt = expiresAt;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }
}
