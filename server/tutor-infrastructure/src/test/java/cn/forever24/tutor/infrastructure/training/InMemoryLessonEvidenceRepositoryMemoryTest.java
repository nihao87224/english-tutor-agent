package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.AttemptAnalysis;
import cn.forever24.tutor.training.AttemptCorrection;
import cn.forever24.tutor.training.AttemptCriterionResult;
import cn.forever24.tutor.training.LessonAttempt;
import cn.forever24.tutor.training.LessonAttemptStatus;
import cn.forever24.tutor.training.TaskAttemptInputType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InMemoryLessonEvidenceRepositoryMemoryTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");

    @Test
    void aggregatesMinimalErrorMemoryPromotesExpressionAndDoesNotReplayDuplicateEvidence() {
        InMemoryLessonEvidenceRepository repository = new InMemoryLessonEvidenceRepository(Clock.fixed(NOW, ZoneOffset.UTC));
        UserKey user = new UserKey("usr-memory");
        repository.record(user, null, attempt("att-1", false));
        repository.record(user, null, attempt("att-1", false));
        repository.record(user, null, attempt("att-2", true));

        var memory = repository.learnerMemory(user, NOW);
        assertEquals(1, memory.weakPoints().size());
        assertEquals(1, memory.weakPoints().getFirst().frequency());
        assertEquals("HIGH", memory.weakPoints().getFirst().severity());
        assertEquals("independent", memory.expressions().getFirst().state().name().toLowerCase());
        String serializedProjection = memory.toString();
        assertFalse(serializedProjection.contains("I has a booking"));
        assertFalse(serializedProjection.contains("I have a booking"));
        assertFalse(serializedProjection.contains("third-party explanation"));
    }

    @Test
    void returnsReviewThatIsDueAtTheFixedClock() {
        InMemoryLessonEvidenceRepository repository = new InMemoryLessonEvidenceRepository(Clock.fixed(NOW, ZoneOffset.UTC));
        UserKey user = new UserKey("usr-due");
        repository.record(user, null, attempt("att-due", true));

        assertFalse(repository.learnerMemory(user, NOW).dueReviews().isEmpty());
    }

    private static LessonAttempt attempt(String id, boolean successful) {
        return new LessonAttempt(id, "session", "task", null, TaskAttemptInputType.TEXT, "learner answer", null, null,
                null, false, LessonAttemptStatus.ACCEPTED, null,
                new AttemptAnalysis("feedback", List.of(new AttemptCriterionResult("meaning", successful, "ok")),
                        successful ? List.of() : List.of(new AttemptCorrection("I has a booking", "I have a booking",
                                "grammar", true, "third-party explanation")),
                        List.of("Could you help me?"), "V2-P0-1", "stub", "stub", "trace"),
                null, Instant.parse("2026-08-01T00:00:00Z"), 1);
    }
}
