package ch.admin.zas.jweb.laforge.tutor.service;

import ch.admin.zas.jweb.laforge.common.error.AiUnavailableException;
import ch.admin.zas.jweb.laforge.practice.domain.Attempt;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Adaptateur par défaut du pilote : aucune IA n'est câblée, toute question échoue avec 503. Une
 * future implémentation réelle remplacera ce bean sans changer {@code TutorService}.
 */
@Component
public class DisabledTutorAdapter implements TutorPort {

    @Override
    public TutorAnswer answer(Attempt attempt, String question) {
        throw new AiUnavailableException("Le tuteur assisté par IA n'est pas disponible dans cette version.", Duration.ofMinutes(5));
    }
}
