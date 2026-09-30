package ch.admin.zas.jweb.laforge.security.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.VerificationToken;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Tests de persistance de {@link VerificationTokenRepository} : round-trip, recherche par
 * empreinte et requête des jetons encore utilisables pour un compte.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
class VerificationTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    private Account persistAccount(String email) {
        return entityManager.persistAndFlush(new Account(email, "hash", "Learner"));
    }

    @Test
    void saveThenFindById_roundTripsAllFields() {
        var account = persistAccount("verify@example.com");
        var expiresAt = OffsetDateTime.parse("2030-01-01T00:00:00Z");
        var token = new VerificationToken(account, "hash-abc", expiresAt);

        var saved = verificationTokenRepository.saveAndFlush(token);
        entityManager.clear();

        var reloaded = verificationTokenRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getAccount().getId()).isEqualTo(account.getId());
        assertThat(reloaded.getTokenHash()).isEqualTo("hash-abc");
        assertThat(reloaded.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(reloaded.getConsumedAt()).isNull();
    }

    @Test
    void findByTokenHash_returnsMatchingToken() {
        var account = persistAccount("hash-lookup@example.com");
        verificationTokenRepository.saveAndFlush(
                new VerificationToken(account, "unique-hash", OffsetDateTime.parse("2030-01-01T00:00:00Z")));

        var found = verificationTokenRepository.findByTokenHash("unique-hash");

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getAccount().getEmail()).isEqualTo("hash-lookup@example.com");
        assertThat(verificationTokenRepository.findByTokenHash("absent")).isEmpty();
    }

    @Test
    void findByAccountAndConsumedAtIsNullAndExpiresAtAfter_excludesConsumedAndExpiredTokens() {
        var account = persistAccount("usable@example.com");
        var now = OffsetDateTime.parse("2025-01-01T00:00:00Z");

        var usable = new VerificationToken(account, "usable", now.plusDays(1));
        var expired = new VerificationToken(account, "expired", now.minusDays(1));
        var consumed = new VerificationToken(account, "consumed", now.plusDays(1));
        consumed.consume(now);

        verificationTokenRepository.saveAllAndFlush(java.util.List.of(usable, expired, consumed));
        entityManager.clear();

        var usableTokens = verificationTokenRepository.findByAccountAndConsumedAtIsNullAndExpiresAtAfter(account, now);

        assertThat(usableTokens).extracting(VerificationToken::getTokenHash).containsExactly("usable");
    }
}
