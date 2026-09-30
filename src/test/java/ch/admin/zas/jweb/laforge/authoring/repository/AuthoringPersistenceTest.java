package ch.admin.zas.jweb.laforge.authoring.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.admin.zas.jweb.laforge.authoring.domain.DraftState;
import ch.admin.zas.jweb.laforge.authoring.dto.ExerciseContentInput;
import ch.admin.zas.jweb.laforge.authoring.service.AuthoringService;
import ch.admin.zas.jweb.laforge.authoring.service.PublicationValidator;
import ch.admin.zas.jweb.laforge.catalog.domain.Correction;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseKind;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseSpec;
import ch.admin.zas.jweb.laforge.catalog.domain.TechnologyRequirement;
import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import ch.admin.zas.jweb.laforge.catalog.repository.ExerciseRepository;
import ch.admin.zas.jweb.laforge.catalog.repository.ExerciseVersionRepository;
import ch.admin.zas.jweb.laforge.catalog.repository.TopicRepository;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.domain.Source;
import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.error.ValidationFailedException;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.common.persistence.RepositoryTestConfig;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.AccountStatus;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import ch.admin.zas.jweb.laforge.practice.repository.AttemptRepository;
import ch.admin.zas.jweb.laforge.practice.service.ExerciseCompletionService;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RepositoryTestConfig.class)
class AuthoringPersistenceTest {

    @Autowired private DraftRepository drafts;
    @Autowired private ExerciseRepository exercises;
    @Autowired private ExerciseVersionRepository versions;
    @Autowired private TopicRepository topics;
    @Autowired private AccountRepository accounts;
    @Autowired private AttemptRepository attempts;
    @Autowired private Clock clock;

    private ValidatorFactory validatorFactory;
    private AuthoringService service;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        service = new AuthoringService(drafts, exercises, versions, topics, accounts,
                new PublicationValidator(validatorFactory.getValidator()), new ExerciseCompletionService(attempts), clock);
    }

    @AfterEach
    void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void titleOnlyDraftCanBeCompletedAndPublishedWithoutReview() {
        var author = account("author@example.com", Role.AUTHOR);
        var draft = service.createDraft(author, null, minimalContent());
        assertThat(draft.revision()).isZero();
        assertThatThrownBy(() -> service.publishDraft(author, draft.id(), draft.revision()))
                .isInstanceOf(ValidationFailedException.class);
        assertThat(versions.count()).isZero();
        assertThat(service.getDraft(author, draft.id()).state()).isEqualTo(DraftState.DRAFT);

        var topic = topics.saveAndFlush(new Topic("java", "Java"));
        var content = new ExerciseContentInput("Titre", ExerciseType.CODE_REVIEW, Difficulty.BEGINNER,
                List.of(topic.getId()), 10, List.of(new TechnologyRequirement("Java", null, null, null)),
                "Question", List.of("Objectif"), null, new ResponseSpec(ResponseKind.FREE_TEXT, null), null,
                new Correction("Explication", null, null, null, null,
                        List.of(new Source("Documentation", "https://example.com/doc", OffsetDateTime.now(clock)))));
        var updated = service.replaceDraft(author, draft.id(), draft.revision(), content);
        assertThat(updated.revision()).isEqualTo(1);
        var published = service.publishDraft(author, draft.id(), updated.revision());
        versions.flush();

        assertThat(published.version()).isEqualTo(1);
        assertThat(published.completed()).isFalse();
        assertThat(published.technologies()).containsExactly(new TechnologyRequirement("Java", null, null, null));
        var saved = service.getDraft(author, draft.id());
        assertThat(saved.state()).isEqualTo(DraftState.PUBLISHED);
        assertThat(saved.revision()).isEqualTo(2);
        assertThat(versions.count()).isEqualTo(1);
    }

    @Test
    void adminOnlyListsAndReadsOwnDrafts() {
        var author = account("owner@example.com", Role.AUTHOR);
        var admin = account("admin@example.com", Role.ADMIN);
        var privateDraft = service.createDraft(author, null, minimalContent());
        var ownDraft = service.createDraft(admin, null, minimalContent());

        assertThat(service.listDrafts(admin, new PageQuery(10, null), null).items())
                .extracting("id").containsExactly(ownDraft.id());
        assertThatThrownBy(() -> service.getDraft(admin, privateDraft.id())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.replaceDraft(admin, privateDraft.id(), 0, minimalContent()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.publishDraft(admin, privateDraft.id(), 0)).isInstanceOf(NotFoundException.class);
    }

    private CurrentAccountDto account(String email, Role role) {
        var account = accounts.saveAndFlush(new Account(email, "hash", "Auteur"));
        return new CurrentAccountDto(account.getId(), email, account.getDisplayName(), AccountStatus.ACTIVE, Set.of(role));
    }

    private ExerciseContentInput minimalContent() {
        return new ExerciseContentInput("Titre", null, null, null, null, null, null, null, null, null, null, null);
    }
}
