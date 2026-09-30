package ch.admin.zas.jweb.laforge.common.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marque une méthode de contrôleur comme exigeant l'en-tête {@code Idempotency-Key} (contrat :
 * clé UUID fournie par le client, unique par compte + méthode + chemin, conservée 24 h). Rejouée
 * avec le même corps, la méthode annotée n'est pas ré-exécutée : la réponse d'origine est
 * retournée telle quelle. Rejouée avec un corps différent, un conflit 409 est levé. Voir
 * {@link IdempotencyAspect} pour la mise en œuvre.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
}
