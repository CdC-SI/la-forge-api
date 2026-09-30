package ch.admin.zas.jweb.laforge.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applique un quota de type seau à jetons à une méthode de contrôleur. La famille détermine à la
 * fois la configuration ({@code laforge.rate-limit.auth}/{@code tutor}) et la clé de quota : par
 * adresse IP pour {@link RateLimitFamily#AUTH} (les routes ne sont pas encore authentifiées), par
 * compte pour {@link RateLimitFamily#TUTOR}. Voir {@link RateLimitAspect}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    RateLimitFamily value();
}
