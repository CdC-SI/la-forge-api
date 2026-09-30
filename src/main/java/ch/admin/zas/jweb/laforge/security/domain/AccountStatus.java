package ch.admin.zas.jweb.laforge.security.domain;

/** Cycle de vie d'un compte, du dépôt de l'inscription à son éventuelle désactivation. */
public enum AccountStatus {
    /** Créé, en attente de confirmation par courriel. */
    PENDING_VERIFICATION,
    /** Activé, peut s'authentifier et pratiquer. */
    ACTIVE,
    /** Verrouillé (ex. tentatives de connexion abusives) ; nécessite une intervention hors API. */
    LOCKED,
    /** Désactivé définitivement. */
    DISABLED
}
