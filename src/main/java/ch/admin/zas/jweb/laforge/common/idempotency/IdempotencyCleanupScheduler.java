package ch.admin.zas.jweb.laforge.common.idempotency;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Purge périodiquement les enregistrements d'idempotence expirés (fenêtre de rejeu de 24 h) afin
 * que la table {@code idempotency_record} ne croisse pas indéfiniment. L'ordonnancement est activé
 * par {@code @EnableScheduling} sur {@link ch.admin.zas.jweb.laforge.common.config.ApplicationConfig}.
 */
@Component
public class IdempotencyCleanupScheduler {

    private final IdempotencyService idempotencyService;

    public IdempotencyCleanupScheduler(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 3_600_000)
    public void purgeExpiredRecords() {
        idempotencyService.purgeExpired();
    }
}
