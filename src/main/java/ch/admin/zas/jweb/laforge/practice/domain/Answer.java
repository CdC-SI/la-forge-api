package ch.admin.zas.jweb.laforge.practice.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Réponse soumise à une tentative, dont la forme dépend du {@code responseSpec} de la version
 * d'exercice tentée. Hiérarchie scellée exhaustive : toute nouvelle variante impose de mettre à
 * jour chaque {@code switch} qui la traite (correction automatique, projection, validation),
 * garanti par le compilateur grâce à l'absence de branche {@code default}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FreeTextAnswer.class, name = "FREE_TEXT"),
        @JsonSubTypes.Type(value = SingleChoiceAnswer.class, name = "SINGLE_CHOICE"),
        @JsonSubTypes.Type(value = MultipleChoiceAnswer.class, name = "MULTIPLE_CHOICE"),
        @JsonSubTypes.Type(value = ReviewAnswer.class, name = "REVIEW")
})
public sealed interface Answer permits FreeTextAnswer, SingleChoiceAnswer, MultipleChoiceAnswer, ReviewAnswer {

    /** Confiance auto-déclarée (1 à 5) commune à toutes les variantes. */
    int confidence();

    /** Raisonnement en texte libre commun à toutes les variantes. */
    String reasoning();
}
