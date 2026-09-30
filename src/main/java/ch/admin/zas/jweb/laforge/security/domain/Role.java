package ch.admin.zas.jweb.laforge.security.domain;

/**
 * Rôles cumulatifs du contrat. Tous les comptes activés peuvent pratiquer (LEARNER) ; les autres
 * rôles sont attribués par l'administration et débloquent les capacités éditoriales.
 */
public enum Role {
    LEARNER,
    AUTHOR,
    ADMIN
}
