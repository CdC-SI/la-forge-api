package ch.admin.zas.jweb.laforge.tutor.service;

import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.error.NotFoundException;
import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.practice.domain.AttemptStatus;
import ch.admin.zas.jweb.laforge.practice.repository.AttemptRepository;
import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.tutor.domain.TutorExchange;
import ch.admin.zas.jweb.laforge.tutor.dto.TutorExchangeDto;
import ch.admin.zas.jweb.laforge.tutor.repository.TutorExchangeRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Historique et sollicitation du tuteur assisté par IA sur une tentative déjà soumise. Contexte
 * strictement limité à cette tentative : aucun accès aux données d'autres apprenants.
 */
@Service
@Transactional(readOnly = true)
public class TutorService {

    private final AttemptRepository attemptRepository;
    private final TutorExchangeRepository tutorExchangeRepository;
    private final TutorPort tutorPort;

    public TutorService(AttemptRepository attemptRepository, TutorExchangeRepository tutorExchangeRepository, TutorPort tutorPort) {
        this.attemptRepository = attemptRepository;
        this.tutorExchangeRepository = tutorExchangeRepository;
        this.tutorPort = tutorPort;
    }

    /**
     * Historique disponible même si l'IA est désactivée. Pagination simplifiée en mémoire : le
     * nombre d'échanges par tentative reste faible dans ce pilote (v1).
     *
     * @throws NotFoundException     si la tentative n'existe pas ou n'appartient pas à l'appelant
     * @throws InvalidStateException si la tentative n'est pas encore soumise
     */
    public Page<TutorExchangeDto> listTutorExchanges(Account account, UUID attemptId, PageQuery pageQuery) {
        var attempt = requireSubmittedOwnedAttempt(account, attemptId);
        var items = tutorExchangeRepository.findByAttemptIdOrderByCreatedAtAscIdAsc(attempt.getId()).stream()
                .map(TutorExchangeDto::from)
                .toList();
        var limited = items.size() > pageQuery.limit() ? items.subList(0, pageQuery.limit()) : items;
        return Page.of(limited, null);
    }

    /**
     * @throws NotFoundException                                                si la tentative n'existe pas ou n'appartient pas à l'appelant
     * @throws InvalidStateException                                            si la tentative n'est pas encore soumise
     * @throws ch.admin.zas.jweb.laforge.common.error.AiUnavailableException si le fournisseur IA est indisponible (503, toujours en v1)
     */
    @Transactional
    public TutorExchangeDto askTutor(Account account, UUID attemptId, String question) {
        var attempt = requireSubmittedOwnedAttempt(account, attemptId);
        var answer = tutorPort.answer(attempt, question);
        var exchange = new TutorExchange(attempt, question, answer.markdown(), answer.sources());
        return TutorExchangeDto.from(tutorExchangeRepository.save(exchange));
    }

    private ch.admin.zas.jweb.laforge.practice.domain.Attempt requireSubmittedOwnedAttempt(Account account, UUID attemptId) {
        var attempt = attemptRepository.findByIdAndLearner_Id(attemptId, account.getId())
                .orElseThrow(() -> new NotFoundException("Tentative introuvable."));
        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw new InvalidStateException("Le tuteur exige une tentative déjà soumise.");
        }
        return attempt;
    }
}
