package ch.admin.zas.jweb.laforge.tutor.service;

import ch.admin.zas.jweb.laforge.common.domain.Source;
import ch.admin.zas.jweb.laforge.practice.domain.Attempt;
import java.util.List;

/**
 * Port vers un fournisseur d'assistance IA, contextualisé à une tentative déjà soumise. Aucune
 * implémentation active en v1 (voir {@link DisabledTutorAdapter}) : le pilote n'expose aucune
 * dépendance réseau sortante vers un modèle de langage.
 */
public interface TutorPort {

    /**
     * Génère une réponse indicative à la question, dans le contexte strict de l'énoncé, du
     * corrigé, de la réponse soumise et de l'historique de la tentative.
     *
     * @throws ch.admin.zas.jweb.laforge.common.error.AiUnavailableException si le fournisseur est indisponible ou désactivé
     */
    TutorAnswer answer(Attempt attempt, String question);

    /** Réponse générée, jamais persistée avant succès complet de l'appel. */
    record TutorAnswer(String markdown, List<Source> sources) {
    }
}
