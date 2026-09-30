package ch.admin.zas.jweb.laforge.common.idempotency;

/** État d'un enregistrement d'idempotence : en cours d'exécution, ou complété avec la réponse à rejouer. */
public enum IdempotencyStatus {
    IN_PROGRESS,
    COMPLETED
}
