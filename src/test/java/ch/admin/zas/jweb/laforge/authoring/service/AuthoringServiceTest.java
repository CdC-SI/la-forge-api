package ch.admin.zas.jweb.laforge.authoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.admin.zas.jweb.laforge.authoring.domain.Draft;
import ch.admin.zas.jweb.laforge.authoring.domain.DraftState;
import ch.admin.zas.jweb.laforge.authoring.dto.ExerciseContentInput;
import ch.admin.zas.jweb.laforge.authoring.repository.DraftRepository;
import ch.admin.zas.jweb.laforge.catalog.domain.Exercise;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseType;
import ch.admin.zas.jweb.laforge.catalog.domain.ExerciseVersion;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseKind;
import ch.admin.zas.jweb.laforge.catalog.domain.ResponseSpec;
import ch.admin.zas.jweb.laforge.catalog.repository.ExerciseRepository;
import ch.admin.zas.jweb.laforge.catalog.repository.ExerciseVersionRepository;
import ch.admin.zas.jweb.laforge.catalog.repository.TopicRepository;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.error.StaleVersionException;
import ch.admin.zas.jweb.laforge.common.error.ValidationFailedException;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.AccountStatus;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import ch.admin.zas.jweb.laforge.practice.service.ExerciseCompletionService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthoringServiceTest {

    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final UUID DRAFT_ID = UUID.randomUUID();
    private static final UUID EXERCISE_ID = UUID.randomUUID();

    @Mock private DraftRepository drafts;
    @Mock private ExerciseRepository exercises;
    @Mock private ExerciseVersionRepository versions;
    @Mock private TopicRepository topics;
    @Mock private AccountRepository accounts;
    @Mock private PublicationValidator validator;
    @Mock private ExerciseCompletionService completionService;

    private AuthoringService service;

    @BeforeEach
    void setUp() {
        service = new AuthoringService(drafts, exercises, versions, topics, accounts, validator, completionService,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void titleOnlyCreatesPrivateDraftWithoutPublicationValidation() {
        var exercise = new Exercise();
        when(exercises.save(any())).thenReturn(exercise);
        when(accounts.getReferenceById(AUTHOR_ID)).thenReturn(mock(Account.class));
        when(drafts.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createDraft(author(Role.AUTHOR), null, minimalContent());

        assertThat(result.state()).isEqualTo(DraftState.DRAFT);
        assertThat(result.content().title()).isEqualTo("Titre");
        assertThat(result.content().responseSpec()).isNull();
        verifyNoInteractions(validator, versions);
    }

    @Test
    void adminCannotReadOrChangeAnotherAuthorsDraft() {
        var admin = author(Role.ADMIN);
        assertThatThrownBy(() -> service.getDraft(admin, DRAFT_ID)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.replaceDraft(admin, DRAFT_ID, 0, minimalContent()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.publishDraft(admin, DRAFT_ID, 0)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(validator, versions);
    }

    @Test
    void adminCannotCreateRevisionOfAnotherAuthorsExercise() {
        assertThatThrownBy(() -> service.createDraft(author(Role.ADMIN), EXERCISE_ID, minimalContent()))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(exercises, versions);
    }

    @Test
    void secondActiveDraftIsRejected() {
        when(drafts.existsByExercise_IdAndAuthor_Id(EXERCISE_ID, AUTHOR_ID)).thenReturn(true);
        when(exercises.findByIdForUpdate(EXERCISE_ID)).thenReturn(Optional.of(new Exercise()));
        when(drafts.findByExerciseIdAndStateNot(EXERCISE_ID, DraftState.PUBLISHED))
                .thenReturn(Optional.of(mock(Draft.class)));
        assertThatThrownBy(() -> service.createDraft(author(Role.AUTHOR), EXERCISE_ID, minimalContent()))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void publishValidatesBeforeCreatingVersion() {
        var draft = ownedDraft();
        doThrow(new ValidationFailedException("Incomplet")).when(validator).validate(any());
        assertThatThrownBy(() -> service.publishDraft(author(Role.AUTHOR), DRAFT_ID, 0))
                .isInstanceOf(ValidationFailedException.class);
        assertThat(draft.getState()).isEqualTo(DraftState.DRAFT);
        verifyNoInteractions(versions);
    }

    @Test
    void publishRejectsStaleRevisionBeforeValidation() {
        ownedDraft();
        assertThatThrownBy(() -> service.publishDraft(author(Role.AUTHOR), DRAFT_ID, 1))
                .isInstanceOf(StaleVersionException.class);
        verifyNoInteractions(validator, versions);
    }

    @Test
    void publishRejectsOutdatedBaseVersion() {
        ownedDraft();
        var latest = mock(ExerciseVersion.class);
        when(latest.getVersionNumber()).thenReturn(1);
        when(versions.findFirstByExercise_IdAndPublishedAtIsNotNullOrderByVersionNumberDesc(EXERCISE_ID))
                .thenReturn(Optional.of(latest));
        assertThatThrownBy(() -> service.publishDraft(author(Role.AUTHOR), DRAFT_ID, 0))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void draftPublishesDirectlyAndCannotBePublishedAgain() {
        var draft = ownedDraft();
        when(versions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(completionService.completedExerciseIds(AUTHOR_ID, List.of(EXERCISE_ID))).thenReturn(Set.of(EXERCISE_ID));
        var published = service.publishDraft(author(Role.AUTHOR), DRAFT_ID, 0);
        assertThat(published.version()).isEqualTo(1);
        assertThat(published.completed()).isTrue();
        assertThat(draft.getState()).isEqualTo(DraftState.PUBLISHED);
        assertThat(draft.getPublishedVersion()).isEqualTo(1);
        verify(validator).validate(any());
        verify(drafts).flush();
        assertThatThrownBy(() -> service.publishDraft(author(Role.AUTHOR), DRAFT_ID, 0))
                .isInstanceOf(InvalidStateException.class);
    }

    private Draft ownedDraft() {
        var exercise = new Exercise();
        org.springframework.test.util.ReflectionTestUtils.setField(exercise, "id", EXERCISE_ID);
        var account = mock(Account.class);
        var draft = new Draft(exercise, account, 0, "Titre", ExerciseType.CODE_REVIEW, Difficulty.BEGINNER,
                10, "Question", List.of("Objectif"), List.of(), List.of(),
                new ResponseSpec(ResponseKind.FREE_TEXT, List.of()), List.of(), null, Set.of());
        when(drafts.findByIdAndAuthor_Id(DRAFT_ID, AUTHOR_ID)).thenReturn(Optional.of(draft));
        return draft;
    }

    private CurrentAccountDto author(Role role) {
        return new CurrentAccountDto(AUTHOR_ID, "author@example.com", "Auteur", AccountStatus.ACTIVE, Set.of(role));
    }

    private ExerciseContentInput minimalContent() {
        return new ExerciseContentInput("Titre", null, null, null, null, null, null, null, null, null, null, null);
    }
}
