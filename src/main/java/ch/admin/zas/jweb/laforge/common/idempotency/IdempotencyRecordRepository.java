package ch.admin.zas.jweb.laforge.common.idempotency;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByIdempotencyKeyAndAccountIdAndHttpMethodAndPath(
            UUID idempotencyKey, UUID accountId, String httpMethod, String path);

    @Modifying
    @Query("delete from IdempotencyRecord r where r.expiresAt < :now")
    int deleteByExpiresAtBefore(OffsetDateTime now);
}
