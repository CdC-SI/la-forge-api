package ch.admin.zas.jweb.laforge.collective.repository;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersionFixtures;
import ch.admin.zas.jweb.laforge.collective.domain.Challenge;
import ch.admin.zas.jweb.laforge.collective.domain.DiscussionComment;
import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Tests de persistance de {@link DiscussionCommentRepository} : round-trip et ordre des
 * commentaires du débrief collectif d'un défi.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
class DiscussionCommentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DiscussionCommentRepository discussionCommentRepository;

    private Account persistAccount(String email) {
        return entityManager.persistAndFlush(new Account(email, "hash", "Learner"));
    }

    private Challenge persistChallenge(Account creator) {
        var exercise = entityManager.persistAndFlush(new Exercise());
        ExerciseVersion version = entityManager.persistAndFlush(ExerciseVersionFixtures.sampleExerciseVersion(exercise, 1, Set.of()));
        return entityManager.persistAndFlush(
                new Challenge("Défi", version, creator, OffsetDateTime.parse("2030-01-01T00:00:00Z"), "hash"));
    }

    @Test
    void saveThenFindById_roundTripsAllFields() {
        var author = persistAccount("author@example.com");
        var challenge = persistChallenge(author);
        var comment = new DiscussionComment(challenge, author, "Bon travail collectif !");

        var saved = discussionCommentRepository.saveAndFlush(comment);
        entityManager.clear();

        var reloaded = discussionCommentRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getChallenge().getId()).isEqualTo(challenge.getId());
        assertThat(reloaded.getAuthor().getId()).isEqualTo(author.getId());
        assertThat(reloaded.getBody()).isEqualTo("Bon travail collectif !");
    }

    @Test
    void findByChallenge_IdOrderByCreatedAtAscIdAsc_ordersChronologically() {
        var author = persistAccount("chrono@example.com");
        var challenge = persistChallenge(author);
        var first = discussionCommentRepository.saveAndFlush(new DiscussionComment(challenge, author, "Premier"));
        var second = discussionCommentRepository.saveAndFlush(new DiscussionComment(challenge, author, "Second"));
        entityManager.clear();

        var comments = discussionCommentRepository.findByChallenge_IdOrderByCreatedAtAscIdAsc(challenge.getId());

        assertThat(comments).extracting(DiscussionComment::getBody).containsExactly("Premier", "Second");
    }
}
