package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.training.EvidenceApplicationService;
import cn.forever24.tutor.application.training.EvidenceSummary;
import cn.forever24.tutor.application.training.LessonAttemptRepository;
import cn.forever24.tutor.application.training.LessonEvidenceRepository;
import cn.forever24.tutor.application.training.LessonSessionRepository;
import cn.forever24.tutor.application.training.LessonSessionStartRecord;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.AttemptAnalysis;
import cn.forever24.tutor.training.AttemptCriterionResult;
import cn.forever24.tutor.training.LessonAttempt;
import cn.forever24.tutor.training.LessonAttemptStatus;
import cn.forever24.tutor.training.LessonInputMode;
import cn.forever24.tutor.training.LessonSession;
import cn.forever24.tutor.training.LessonSessionStatus;
import cn.forever24.tutor.training.LessonStep;
import cn.forever24.tutor.training.TaskAttemptInputType;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EvidenceApplicationTransactionTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");
    private static final UserKey USER = new UserKey("usr-transaction");

    @Test
    void rollsBackTheEvidenceConsumerProjectionWhenAttemptTransitionFails() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:evidence_transaction;MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE learner_memory_probe (attempt_id VARCHAR(64) PRIMARY KEY)");
        LessonEvidenceRepository evidence = (user, session, attempt) -> {
            jdbc.update("INSERT INTO learner_memory_probe (attempt_id) VALUES (?)", attempt.attemptId());
            return new EvidenceSummary(attempt.attemptId(), 1, List.of("speaking"), "focus");
        };
        EvidenceApplicationService service = new EvidenceApplicationService(sessionRepository(), failingAttemptRepository(), evidence,
                new SpringLessonSessionTransactionOperations(new TransactionTemplate(new DataSourceTransactionManager(dataSource))),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(IllegalStateException.class, () -> service.finalizeFeedback(USER.value(), "session", "attempt"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM learner_memory_probe", Integer.class));
    }

    private static LessonSessionRepository sessionRepository() {
        return new LessonSessionRepository() {
            @Override public Optional<LessonSessionStartRecord> findStartForUpdate(UserKey userKey, String idempotencyKey) { return Optional.empty(); }
            @Override public void insert(UserKey userKey, LessonSession session, String idempotencyKey, String requestHash) { }
            @Override public Optional<LessonSession> findById(UserKey userKey, String sessionId) { return Optional.of(session()); }
            @Override public LessonSession save(UserKey userKey, long expectedVersion, LessonSession session) { return session; }
        };
    }

    private static LessonAttemptRepository failingAttemptRepository() {
        return new LessonAttemptRepository() {
            @Override public Optional<cn.forever24.tutor.application.training.LessonAttemptStoreRecord> findByIdempotencyKey(UserKey userKey, String sessionId, String idempotencyKey) { return Optional.empty(); }
            @Override public Optional<LessonAttempt> findById(UserKey userKey, String sessionId, String attemptId) { return Optional.of(attempt()); }
            @Override public List<LessonAttempt> findBySession(UserKey userKey, String sessionId) { return List.of(attempt()); }
            @Override public void insert(UserKey userKey, LessonAttempt attempt, String idempotencyKey, String requestHash) { }
            @Override public void updateAnalysis(UserKey userKey, LessonAttempt attempt, long expectedVersion) { throw new IllegalStateException("force rollback"); }
        };
    }

    private static LessonSession session() {
        List<LessonStep> steps = List.of(LessonStep.SCENE_CONTEXT, LessonStep.FIRST_LISTEN, LessonStep.COMPREHENSION,
                LessonStep.TRANSCRIPT_EXPRESSIONS, LessonStep.GUIDED_SPEAKING, LessonStep.FEEDBACK, LessonStep.EVIDENCE, LessonStep.COMPLETE);
        return new LessonSession("session", "prescription", 1, "block", "resource", "version", "variant", "episode",
                LessonInputMode.VOICE_OR_TEXT, LessonSessionStatus.IN_PROGRESS, LessonStep.FEEDBACK, steps,
                steps.subList(0, 5), NOW, null, null, 1);
    }

    private static LessonAttempt attempt() {
        return new LessonAttempt("attempt", "session", "task", null, TaskAttemptInputType.TEXT, "answer", null, null, null,
                false, LessonAttemptStatus.ACCEPTED, null,
                new AttemptAnalysis("feedback", List.of(new AttemptCriterionResult("meaning", true, "ok")), List.of(), List.of(),
                        "V2-P0-1", "stub", "stub", "trace"), null, NOW, 1);
    }
}
