package ch.admin.zas.jweb.laforge.security.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.RefreshToken;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Tests de persistance de {@link RefreshTokenRepository} : round-trip et regroupement par
 * {@code familyId} (utilisé pour révoquer toute une famille en cas de vol de jeton détecté).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
class RefreshTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private Account persistAccount(String email) {
        return entityManager.persistAndFlush(new Account(email, "hash", "Learner"));
    }

    @Test
    void saveThenFindById_roundTripsAllFields() {
        var account = persistAccount("refresh@example.com");
        var familyId = UUID.randomUUID();
        var expiresAt = OffsetDateTime.parse("2030-01-01T00:00:00Z");
        var token = new RefreshToken(account, "hash-abc", familyId, expiresAt);

        var saved = refreshTokenRepository.saveAndFlush(token);
        entityManager.clear();

        var reloaded = refreshTokenRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getAccount().getId()).isEqualTo(account.getId());
        assertThat(reloaded.getTokenHash()).isEqualTo("hash-abc");
        assertThat(reloaded.getFamilyId()).isEqualTo(familyId);
        assertThat(reloaded.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(reloaded.getRevokedAt()).isNull();
    }

    @Test
    void findByTokenHash_returnsMatchingToken() {
        var account = persistAccount("hash-lookup@example.com");
        refreshTokenRepository.saveAndFlush(
                new RefreshToken(account, "unique-hash", UUID.randomUUID(), OffsetDateTime.parse("2030-01-01T00:00:00Z")));

        assertThat(refreshTokenRepository.findByTokenHash("unique-hash")).isPresent();
        assertThat(refreshTokenRepository.findByTokenHash("absent")).isEmpty();
    }

    @Test
    void findByFamilyId_returnsAllTokensOfTheFamily() {
        var account = persistAccount("family@example.com");
        var familyId = UUID.randomUUID();
        var otherFamilyId = UUID.randomUUID();
        var expiresAt = OffsetDateTime.parse("2030-01-01T00:00:00Z");

        var first = new RefreshToken(account, "first", familyId, expiresAt);
        var second = new RefreshToken(account, "second", familyId, expiresAt);
        var other = new RefreshToken(account, "other-family", otherFamilyId, expiresAt);
        refreshTokenRepository.saveAllAndFlush(List.of(first, second, other));
        entityManager.clear();

        List<RefreshToken> family = refreshTokenRepository.findByFamilyId(familyId);

        assertThat(family).extracting(RefreshToken::getTokenHash).containsExactlyInAnyOrder("first", "second");
    }
}
