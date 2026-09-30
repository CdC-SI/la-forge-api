package ch.admin.zas.jweb.laforge.security.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injecte le {@code Account} authentifié dans un paramètre de contrôleur, résolu à partir du
 * claim {@code sub} du JWT courant — jamais à partir du corps de la requête.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentAccount {
}
