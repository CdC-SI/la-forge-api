package ch.admin.zas.jweb.laforge.security.repository;

import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.VerificationToken;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByTokenHash(String tokenHash);

    List<VerificationToken> findByAccountAndConsumedAtIsNullAndExpiresAtAfter(Account account, OffsetDateTime now);
}
