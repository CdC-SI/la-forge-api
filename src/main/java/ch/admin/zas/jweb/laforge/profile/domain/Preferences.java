package ch.admin.zas.jweb.laforge.profile.domain;

import ch.admin.zas.jweb.laforge.catalog.domain.Topic;
import ch.admin.zas.jweb.laforge.common.domain.Difficulty;
import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import ch.admin.zas.jweb.laforge.profile.dto.StackEntryDto;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Préférences de pratique d'un compte (schéma {@code Preferences}). Créées paresseusement à la
 * première lecture ou écriture, avec les valeurs par défaut d'un compte fraîchement activé.
 */
@Entity
@Table(name = "preferences", uniqueConstraints = @UniqueConstraint(name = "uk_preferences_account", columnNames = "account_id"))
public class Preferences extends BaseEntity {

    public static final int DEFAULT_SESSION_MINUTES = 15;
    public static final String DEFAULT_LOCALE = "fr";
    public static final String DEFAULT_TIME_ZONE = "Europe/Zurich";

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Convert(converter = StackEntriesConverter.class)
    @Column(name = "stack", columnDefinition = "jsonb")
    private List<StackEntryDto> stack;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "preferences_topic",
            joinColumns = @JoinColumn(name = "preferences_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id"))
    private Set<Topic> topics = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(name = "session_minutes", nullable = false)
    private int sessionMinutes;

    @Column(name = "locale", nullable = false, length = 5)
    private String locale;

    @Column(name = "time_zone", nullable = false, length = 100)
    private String timeZone;

    protected Preferences() {
        // Requis par JPA.
    }

    public Preferences(Account account) {
        this.account = account;
        this.stack = List.of();
        this.topics = new LinkedHashSet<>();
        this.difficulty = Difficulty.INTERMEDIATE;
        this.sessionMinutes = DEFAULT_SESSION_MINUTES;
        this.locale = DEFAULT_LOCALE;
        this.timeZone = DEFAULT_TIME_ZONE;
    }

    /** Remplacement intégral (le contrat n'expose qu'un {@code PUT}, jamais de correctif partiel). */
    public void replaceWith(
            List<StackEntryDto> newStack,
            Set<Topic> newTopics,
            Difficulty newDifficulty,
            int newSessionMinutes,
            String newLocale,
            String newTimeZone) {
        this.stack = newStack == null ? List.of() : List.copyOf(newStack);
        this.topics = new LinkedHashSet<>(newTopics);
        this.difficulty = newDifficulty;
        this.sessionMinutes = newSessionMinutes;
        this.locale = newLocale;
        this.timeZone = newTimeZone;
    }

    public Account getAccount() {
        return account;
    }

    public List<StackEntryDto> getStack() {
        return stack;
    }

    public Set<Topic> getTopics() {
        return Set.copyOf(topics);
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public int getSessionMinutes() {
        return sessionMinutes;
    }

    public String getLocale() {
        return locale;
    }

    public String getTimeZone() {
        return timeZone;
    }
}
