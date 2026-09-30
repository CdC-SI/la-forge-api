package ch.admin.zas.jweb.laforge.security.domain;

/**
 * Rôles cumulatifs du contrat. Tous les comptes activés peuvent pratiquer (LEARNER) ; les autres
 * rôles s'ajoutent hors API (gestion manuelle) et débloquent les capacités éditoriales.
 */
public enum Role {
    LEARNER,
    AUTHOR,
    REVIEWER,
    ADMIN
}
