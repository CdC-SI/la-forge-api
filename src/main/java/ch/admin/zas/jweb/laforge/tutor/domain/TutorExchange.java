package ch.admin.zas.jweb.laforge.tutor.domain;

import ch.admin.zas.jweb.laforge.common.domain.Source;
import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import ch.admin.zas.jweb.laforge.practice.domain.Attempt;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Objects;

/**
 * Échange ponctuel avec le tuteur assisté par IA, à propos d'une tentative déjà soumise. Persisté
 * uniquement après une génération réussie (le contrat n'expose que {@code generatedByAi = true}) ;
 * en cas d'indisponibilité du fournisseur, rien n'est enregistré et l'appelant reçoit 503.
 */
@Entity
@Table(name = "tutor_exchange")
public class TutorExchange extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @Column(name = "question", nullable = false, length = 4000)
    private String question;

    @Column(name = "answer_markdown", nullable = false, length = 20000)
    private String answerMarkdown;

    @Convert(converter = TutorSourcesConverter.class)
    @Column(name = "sources", columnDefinition = "jsonb")
    private List<Source> sources;

    protected TutorExchange() {
        // Requis par JPA.
    }

    public TutorExchange(Attempt attempt, String question, String answerMarkdown, List<Source> sources) {
        this.attempt = Objects.requireNonNull(attempt, "attempt");
        this.question = Objects.requireNonNull(question, "question");
        this.answerMarkdown = Objects.requireNonNull(answerMarkdown, "answerMarkdown");
        this.sources = sources == null ? List.of() : List.copyOf(sources);
    }

    public Attempt getAttempt() {
        return attempt;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswerMarkdown() {
        return answerMarkdown;
    }

    public List<Source> getSources() {
        return sources;
    }

    /** Toujours vrai : seules les réponses générées par le fournisseur IA sont persistées. */
    public boolean isGeneratedByAi() {
        return true;
    }
}
