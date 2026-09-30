package ch.admin.zas.jweb.laforge.common.idempotency;

import ch.admin.zas.jweb.laforge.common.error.IdempotencyConflictException;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applique la règle de déduplication décrite par le contrat pour les mutations porteuses de
 * l'en-tête {@code Idempotency-Key} : même clé et même corps rejouent la réponse d'origine sans
 * répéter l'effet ; même clé et corps différent lève un conflit 409 ; une opération encore en
 * cours pour cette clé lève également un 409 invitant à réessayer avec la même clé. Les
 * enregistrements complétés sont conservés 24 h.
 */
@Service
public class IdempotencyService {

    private static final Duration RETENTION = Duration.ofHours(24);

    private final IdempotencyRecordRepository repository;
    private final Clock clock;

    public IdempotencyService(IdempotencyRecordRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Réponse mise en cache à rejouer telle quelle, ou vide si l'appelant doit exécuter l'opération. */
    public record CachedResponse(int status, String body) {
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<CachedResponse> begin(
            UUID idempotencyKey, UUID accountId, String httpMethod, String path, String requestHash) {
        var existing =
                repository.findByIdempotencyKeyAndAccountIdAndHttpMethodAndPath(idempotencyKey, accountId, httpMethod, path);
        if (existing.isPresent()) {
            var record = existing.get();
            if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                throw new InvalidStateException(
                        "Une opération est déjà en cours pour cette clé d'idempotence ; réessayez avec la même clé.");
            }
            if (!record.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                        "La clé d'idempotence a déjà été utilisée avec un corps de requête différent.");
            }
            return Optional.of(new CachedResponse(record.getResponseStatus(), record.getResponseBody()));
        }
        var now = OffsetDateTime.now(clock);
        try {
            repository.save(
                    IdempotencyRecord.beginning(idempotencyKey, accountId, httpMethod, path, requestHash, now.plus(RETENTION)));
        } catch (DataIntegrityViolationException raceLost) {
            throw new InvalidStateException(
                    "Une opération est déjà en cours pour cette clé d'idempotence ; réessayez avec la même clé.");
        }
        return Optional.empty();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(UUID idempotencyKey, UUID accountId, String httpMethod, String path, int status, String body) {
        repository
                .findByIdempotencyKeyAndAccountIdAndHttpMethodAndPath(idempotencyKey, accountId, httpMethod, path)
                .ifPresent(record -> record.complete(status, body, OffsetDateTime.now(clock).plus(RETENTION)));
    }

    /** Libère l'enregistrement après un échec afin que le client puisse réessayer immédiatement avec la même clé. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(UUID idempotencyKey, UUID accountId, String httpMethod, String path) {
        repository
                .findByIdempotencyKeyAndAccountIdAndHttpMethodAndPath(idempotencyKey, accountId, httpMethod, path)
                .filter(record -> record.getStatus() == IdempotencyStatus.IN_PROGRESS)
                .ifPresent(repository::delete);
    }

    /** Purge périodique des enregistrements expirés (voir {@link IdempotencyCleanupScheduler}). */
    @Transactional
    public int purgeExpired() {
        return repository.deleteByExpiresAtBefore(OffsetDateTime.now(clock));
    }
}
