package cn.forever24.tutor.application.training;

import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonAttempt;
import cn.forever24.tutor.training.LessonAttemptStatus;
import cn.forever24.tutor.training.LessonSession;

import java.time.Clock;
import java.util.Objects;

/** Commits evidence, learner-skill updates and the Attempt transition as one replay-safe operation. */
public final class EvidenceApplicationService {
    private final LessonSessionRepository sessionRepository;
    private final LessonAttemptRepository attemptRepository;
    private final LessonEvidenceRepository evidenceRepository;
    private final LessonSessionTransactionOperations transactions;
    private final Clock clock;

    public EvidenceApplicationService(LessonSessionRepository sessionRepository, LessonAttemptRepository attemptRepository,
                                      LessonEvidenceRepository evidenceRepository,
                                      LessonSessionTransactionOperations transactions, Clock clock) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository);
        this.attemptRepository = Objects.requireNonNull(attemptRepository);
        this.evidenceRepository = Objects.requireNonNull(evidenceRepository);
        this.transactions = Objects.requireNonNull(transactions);
        this.clock = Objects.requireNonNull(clock);
    }

    public EvidenceSummary finalizeFeedback(String userKeyValue, String sessionId, String attemptId) {
        UserKey userKey = new UserKey(userKeyValue);
        return transactions.execute(() -> {
            LessonSession session = sessionRepository.findByIdForUpdate(userKey, sessionId)
                    .orElseThrow(LessonSessionApplicationException::notFound);
            LessonAttempt attempt = attemptRepository.findById(userKey, sessionId, attemptId)
                    .orElseThrow(LessonSessionApplicationException::attemptNotFound);
            if (attempt.status() == LessonAttemptStatus.EVIDENCE_RECORDED) {
                return evidenceRepository.record(userKey, session, attempt);
            }
            if (attempt.status() != LessonAttemptStatus.ACCEPTED || attempt.analysis() == null) {
                throw LessonSessionApplicationException.stateConflict("validated accepted feedback is required before evidence");
            }
            LessonSession evidenceSession = session.beginEvidence();
            EvidenceSummary summary = evidenceRepository.record(userKey, evidenceSession, attempt);
            LessonAttempt recorded = attempt.withEvidenceRecorded();
            attemptRepository.updateAnalysis(userKey, recorded, attempt.version());
            LessonSession completed = evidenceSession.completeEvidence(clock.instant());
            sessionRepository.save(userKey, session.version(), completed);
            return summary;
        });
    }
}
