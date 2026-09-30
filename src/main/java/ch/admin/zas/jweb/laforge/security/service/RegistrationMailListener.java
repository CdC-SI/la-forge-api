package ch.admin.zas.jweb.laforge.security.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Déclenche l'envoi du courriel de vérification après le commit de la transaction qui a persisté
 * le jeton, et de façon asynchrone pour ne pas ralentir la réponse HTTP.
 */
@Component
public class RegistrationMailListener {

    private final MailNotifier mailNotifier;

    public RegistrationMailListener(MailNotifier mailNotifier) {
        this.mailNotifier = mailNotifier;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationTokenIssued(VerificationTokenIssuedEvent event) {
        mailNotifier.sendVerificationEmail(event.email(), event.displayName(), event.rawToken());
    }
}
