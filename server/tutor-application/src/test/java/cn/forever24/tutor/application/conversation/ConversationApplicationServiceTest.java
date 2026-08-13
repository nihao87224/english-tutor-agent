package cn.forever24.tutor.application.conversation;

import cn.forever24.tutor.application.planning.LearningPlanRepository;
import cn.forever24.tutor.application.training.TrainingSessionCompletion;
import cn.forever24.tutor.application.training.TrainingSessionRepository;
import cn.forever24.tutor.learner.LearningEvidenceDraft;
import cn.forever24.tutor.planning.LearningPlan;
import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.TaskAttemptReceipt;
import cn.forever24.tutor.training.TaskAttemptSubmission;
import cn.forever24.tutor.training.TrainingSession;
import cn.forever24.tutor.training.TrainingSessionMode;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversationApplicationServiceTest {

    private static final LearningPlan PLAN = new LearningPlan(
            "plan-1",
            LocalDate.parse("2026-08-10"),
            10,
            List.of(new LearningPlanTask(
                    "task-1",
                    "CONVERSATION",
                    "Ask a follow-up question",
                    10,
                    List.of("speaking"),
                    "A2",
                    "Practice natural follow-up questions.")),
            List.of("Conversation practice."),
            false,
            1);

    private final FakeTrainingSessionRepository trainingSessionRepository = new FakeTrainingSessionRepository();
    private final ConversationApplicationService service = new ConversationApplicationService(
            trainingSessionRepository,
            new FakeLearningPlanRepository(),
            context -> List.of(
                    ConversationStreamEvent.status(1, "THINKING", "Thinking"),
                    ConversationStreamEvent.textDelta(2, "Nice work."),
                    ConversationStreamEvent.correctionReady(3, new LayeredCorrectionResult(
                            false,
                            List.of(),
                            "Clear response.",
                            "correction-analyzer-v1",
                            "correction-result-v1",
                            "trace-correction-1",
                            "fake",
                            "fake-chat")),
                    ConversationStreamEvent.done(4, "trace-1", "fake", "fake-chat")));

    @Test
    void streamsConversationForActiveCurrentTask() {
        List<ConversationStreamEvent> events = service.streamMessage(new ConversationStreamRequest(
                "user-1",
                "training-1",
                ConversationMessageType.TEXT,
                "Today I fixed a database connection issue.",
                "task-1",
                "conversation-idem-1"));

        assertEquals("status", events.get(0).type().eventName());
        assertEquals("text_delta", events.get(1).type().eventName());
        assertEquals("correction_ready", events.get(2).type().eventName());
        assertEquals("done", events.get(3).type().eventName());
    }

    @Test
    void rejectsInvalidMessageAndSessionState() {
        assertThrows(IllegalArgumentException.class, () -> service.streamMessage(new ConversationStreamRequest(
                "user-1",
                "training-1",
                ConversationMessageType.AUDIO,
                null,
                "task-1",
                "conversation-idem-1")));

        trainingSessionRepository.session = trainingSessionRepository.session.pause(Instant.parse("2026-08-10T08:01:00Z"));
        assertThrows(IllegalArgumentException.class, () -> service.streamMessage(new ConversationStreamRequest(
                "user-1",
                "training-1",
                ConversationMessageType.TEXT,
                "Hello",
                "task-1",
                "conversation-idem-2")));
    }

    @Test
    void rejectsOtherUserAndNonCurrentTask() {
        assertThrows(IllegalArgumentException.class, () -> service.streamMessage(new ConversationStreamRequest(
                "user-2",
                "training-1",
                ConversationMessageType.TEXT,
                "Hello",
                "task-1",
                "conversation-idem-1")));
        assertThrows(IllegalArgumentException.class, () -> service.streamMessage(new ConversationStreamRequest(
                "user-1",
                "training-1",
                ConversationMessageType.TEXT,
                "Hello",
                "task-2",
                "conversation-idem-2")));
    }

    private static final class FakeTrainingSessionRepository implements TrainingSessionRepository {

        private TrainingSession session = TrainingSession.startDaily(
                "training-1",
                "plan-1",
                TrainingSessionMode.TEXT,
                "task-1",
                Instant.parse("2026-08-10T08:00:00Z"));

        @Override
        public TrainingSession startDailySession(
                UserKey userKey,
                LearningPlan plan,
                TrainingSessionMode mode,
                String idempotencyKey
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<TrainingSession> findById(UserKey userKey, String sessionId) {
            if ("user-1".equals(userKey.value()) && session.sessionId().equals(sessionId)) {
                return Optional.of(session);
            }
            return Optional.empty();
        }

        @Override
        public TrainingSession save(UserKey userKey, TrainingSession session) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TrainingSessionCompletion completeSession(UserKey userKey, TrainingSession session) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskAttemptReceipt submitTextAttempt(
                UserKey userKey,
                TrainingSession session,
                LearningPlanTask task,
                TaskAttemptSubmission submission,
                List<LearningEvidenceDraft> evidence,
                String idempotencyKey,
                String nextTaskId
        ) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeLearningPlanRepository implements LearningPlanRepository {

        @Override
        public LearningPlan getOrGenerateTodayPlan(UserKey userKey, LocalDate planDate) {
            throw new UnsupportedOperationException();
        }

        @Override
        public LearningPlan getPlan(UserKey userKey, String planId) {
            return PLAN;
        }

        @Override
        public void recordTrainingCompletion(UserKey userKey, String planId, List<String> practicedSkills, int evidenceCount) {
            throw new UnsupportedOperationException();
        }
    }
}
