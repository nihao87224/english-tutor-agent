package cn.forever24.tutor.training;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrainingSessionTest {

    private final Instant now = Instant.parse("2026-08-10T08:00:00Z");

    @Test
    void startsDailySessionInProgressWithFirstTask() {
        TrainingSession session = TrainingSession.startDaily(
                "training-1",
                "plan-1",
                TrainingSessionMode.TEXT,
                "task-1",
                now);

        assertEquals(TrainingSessionStatus.IN_PROGRESS, session.status());
        assertEquals("task-1", session.currentTaskId());
        assertEquals(TrainingSessionType.DAILY, session.type());
        assertEquals(TrainingSessionMode.TEXT, session.mode());
    }

    @Test
    void pauseResumeAndCompleteFollowStateMachine() {
        TrainingSession session = TrainingSession.startDaily(
                "training-1",
                "plan-1",
                TrainingSessionMode.MIXED,
                "task-1",
                now);

        TrainingSession paused = session.pause(now.plusSeconds(30));
        TrainingSession resumed = paused.resume();
        TrainingSession completed = resumed.complete(now.plusSeconds(60));

        assertEquals(TrainingSessionStatus.PAUSED, paused.status());
        assertEquals(TrainingSessionStatus.IN_PROGRESS, resumed.status());
        assertEquals(TrainingSessionStatus.COMPLETED, completed.status());
    }

    @Test
    void rejectsInvalidTransitions() {
        TrainingSession completed = TrainingSession.startDaily(
                "training-1",
                "plan-1",
                TrainingSessionMode.MIXED,
                "task-1",
                now).complete(now.plusSeconds(60));

        assertThrows(IllegalStateException.class, () -> completed.pause(now.plusSeconds(90)));
        assertThrows(IllegalStateException.class, completed::resume);
        assertThrows(IllegalStateException.class, () -> completed.complete(now.plusSeconds(90)));
    }
}
