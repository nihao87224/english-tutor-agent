package cn.forever24.tutor.application.roleplay;

import cn.forever24.tutor.application.quota.*;
import cn.forever24.tutor.application.training.*;
import cn.forever24.tutor.curriculum.TrainingType;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.*;
import java.util.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RolePlayApplicationServiceTest {
    private final LessonSessionRepository sessions = mock(LessonSessionRepository.class);
    private final LessonAttemptApplicationService attempts = mock(LessonAttemptApplicationService.class);
    private final LessonContentReader content = mock(LessonContentReader.class);
    private final FakeTurnRepository turns = new FakeTurnRepository();
    private final RolePlayResponder responder = mock(RolePlayResponder.class);
    private final DailyQuotaApplicationService quota = mock(DailyQuotaApplicationService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-21T02:00:00Z"), ZoneOffset.UTC);
    private RolePlayApplicationService service;

    @BeforeEach
    void setUp() {
        LessonSession session = rolePlaySession();
        when(sessions.findById(any(), eq("lsn-1"))).thenReturn(Optional.of(session));
        when(sessions.findByIdForUpdate(any(), eq("lsn-1"))).thenReturn(Optional.of(session));
        when(content.read("season1.ep006", "1.0.0")).thenReturn(lessonContent());
        when(attempts.submit(anyString(), eq("lsn-1"), any(), anyString()))
                .thenReturn(new LessonAttemptMutationResult(textAttempt(),
                        new LessonAttemptProgress(LessonStep.ROLE_PLAY, List.of(), List.of(), false, "att-1"), false));
        when(quota.reserve(anyString(), eq(QuotaRequestType.ROLE_PLAY_REPLY), anyString()))
                .thenReturn(new QuotaReservation("qr-1", "usr-1", LocalDate.parse("2026-08-21"),
                        QuotaRequestType.ROLE_PLAY_REPLY, "turn-1:1", QuotaReservationStatus.RESERVED));
        when(responder.respond(any())).thenReturn(new RolePlayResponse(
                List.of("Let me check ", "Gate 24."), "role-play-lin-muen-v1",
                "stub", "stub-model", "trace-1"));
        service = new RolePlayApplicationService(
                sessions, attempts, content, turns, new DirectTransactions(), responder, quota, clock);
    }

    @Test
    void persistsAcceptedTurnAndStreamsWithinLockedPrescriptionBoundary() {
        RolePlayStreamResult result = service.stream("usr-1", "lsn-1", command("Gate 24, right?"), "idem-1");

        assertEquals(List.of("turn.accepted", "reply.delta", "reply.completed", "analysis.pending"),
                result.events().stream().map(event -> event.type().eventName()).toList());
        RolePlayTurn stored = turns.findByTurnId(new UserKey("usr-1"), "lsn-1", "turn-1").orElseThrow().turn();
        assertEquals(RolePlayTurnStatus.COMPLETED, stored.status());
        assertEquals("Let me check Gate 24.", stored.replyText());
        ArgumentCaptor<RolePlayResponseContext> boundary = ArgumentCaptor.forClass(RolePlayResponseContext.class);
        verify(responder).respond(boundary.capture());
        assertEquals("travel.confirm_gate_change.b1", boundary.getValue().skillUnitVariantId());
        assertEquals("map-airport", boundary.getValue().episodeMappingId());
        assertEquals("Confirm the gate and boarding time", boundary.getValue().task().goal());
        verify(quota).commit(any());
        verify(sessions, never()).save(any(), anyLong(), any());
    }

    @Test
    void replaysCompletedDuplicateAndRejectsChangedPayload() {
        service.stream("usr-1", "lsn-1", command("Gate 24, right?"), "idem-1");
        RolePlayStreamResult replay = service.stream("usr-1", "lsn-1", command("Gate 24, right?"), "idem-1");
        assertTrue(replay.replayed());
        verify(responder, times(1)).respond(any());

        LessonSessionApplicationException conflict = assertThrows(LessonSessionApplicationException.class,
                () -> service.stream("usr-1", "lsn-1", command("Change the goal"), "idem-1"));
        assertEquals("IDEMPOTENCY_CONFLICT", conflict.code());
    }

    @Test
    void rejectsASecondIdempotencyKeyForTheSameConversationTurnBeforeCreatingAnAttempt() {
        service.stream("usr-1", "lsn-1", command("Gate 24, right?"), "idem-1");

        LessonSessionApplicationException conflict = assertThrows(LessonSessionApplicationException.class,
                () -> service.stream("usr-1", "lsn-1", command("Gate 24, right?"), "idem-2"));

        assertEquals("IDEMPOTENCY_CONFLICT", conflict.code());
        verify(attempts, times(1)).submit(anyString(), eq("lsn-1"), any(), anyString());
    }

    @Test
    void keepsAttemptAndReturnsRetryableStreamErrorWhenProviderFails() {
        when(responder.respond(any())).thenThrow(
                new RolePlayResponderException("AI_TEMPORARILY_UNAVAILABLE", true, "timeout"));
        RolePlayStreamResult result = service.stream("usr-1", "lsn-1", command("Gate 24?"), "idem-1");
        assertEquals("stream.error", result.events().getLast().type().eventName());
        assertEquals(true, result.events().getLast().data().get("retryable"));
        assertEquals(RolePlayTurnStatus.FAILED_RETRYABLE,
                turns.findByTurnId(new UserKey("usr-1"), "lsn-1", "turn-1").orElseThrow().turn().status());
        verify(quota).refund(any());
    }

    @Test
    void reportsQuotaFailureAfterAcceptanceWithoutCallingProvider() {
        when(quota.reserve(anyString(), eq(QuotaRequestType.ROLE_PLAY_REPLY), anyString()))
                .thenThrow(QuotaException.exceeded(new DailyQuotaStatus(
                        LocalDate.parse("2026-08-21"), 1, 1, 0, 0, false,
                        OffsetDateTime.parse("2026-08-22T00:00:00+08:00"))));
        RolePlayStreamResult result = service.stream("usr-1", "lsn-1", command("Gate 24?"), "idem-1");
        assertEquals("QUOTA_EXCEEDED", result.events().getLast().data().get("code"));
        verifyNoInteractions(responder);
    }

    @Test
    void requiresConfirmationBeforeAudioTurnCanReachProvider() {
        LessonAttempt audio = new LessonAttempt(
                "att-1", "lsn-1", "gate-role", TaskAttemptInputType.AUDIO, null, "audio-1",
                "Gate twenty four", 0.40, false, LessonAttemptStatus.RECEIVED, null, clock.instant(), 2);
        when(attempts.submit(anyString(), eq("lsn-1"), any(), anyString()))
                .thenReturn(new LessonAttemptMutationResult(audio,
                        new LessonAttemptProgress(LessonStep.ROLE_PLAY, List.of(), List.of(), false, "att-1"), false));
        RolePlayMessageCommand command = new RolePlayMessageCommand("gate-role", null, "audio-1", "turn-1");
        RolePlayStreamResult result = service.stream("usr-1", "lsn-1", command, "idem-1");
        assertEquals("ASR_CONFIRMATION_REQUIRED", result.events().getLast().data().get("code"));
        verifyNoInteractions(responder);
    }

    @Test
    void rejectsRoleOrGoalOutsideLockedTask() {
        LessonSessionApplicationException failure = assertThrows(LessonSessionApplicationException.class,
                () -> service.stream("usr-1", "lsn-1",
                        new RolePlayMessageCommand("foreign-task", "Hello", null, "turn-1"), "idem-1"));
        assertEquals("SESSION_STATE_CONFLICT", failure.code());
    }

    private static RolePlayMessageCommand command(String text) {
        return new RolePlayMessageCommand("gate-role", text, null, "turn-1");
    }

    private LessonAttempt textAttempt() {
        return new LessonAttempt("att-1", "lsn-1", "gate-role", TaskAttemptInputType.TEXT,
                "Gate 24, right?", null, null, null, false,
                LessonAttemptStatus.ANALYSIS_PENDING, null, clock.instant(), 1);
    }

    private static LessonContent lessonContent() {
        return new LessonContent(List.of(), List.of(), new RolePlayTask(
                "gate-role", "Confirm the gate and boarding time", "Traveler helping Lin Muen",
                "Airport agent", List.of("Confirm Gate 24", "Confirm 3:20"), "How can I help?"));
    }

    private static LessonSession rolePlaySession() {
        LessonSession session = LessonSession.start(
                "lsn-1", "prx-1", 1, "block-1", "season1.ep006", "1.0.0",
                "travel.confirm_gate_change.b1", "map-airport", TrainingType.ROLE_PLAY,
                LessonInputMode.VOICE_OR_TEXT, Instant.parse("2026-08-21T01:00:00Z"));
        return session.completeDeterministicStep(LessonStep.SCENE_CONTEXT)
                .completeDeterministicStep(LessonStep.FIRST_LISTEN)
                .completeAttemptStep(LessonStep.COMPREHENSION)
                .completeDeterministicStep(LessonStep.TRANSCRIPT_EXPRESSIONS)
                .completeAttemptStep(LessonStep.GUIDED_SPEAKING);
    }

    private static final class DirectTransactions implements LessonSessionTransactionOperations {
        @Override public <T> T execute(Supplier<T> action) { return action.get(); }
    }

    private static final class FakeTurnRepository implements RolePlayTurnRepository {
        private final Map<String, RolePlayTurnStoreRecord> byTurn = new HashMap<>();
        private final Map<String, RolePlayTurnStoreRecord> byKey = new HashMap<>();
        public Optional<RolePlayTurnStoreRecord> findByIdempotencyKey(UserKey user, String session, String key) {
            return Optional.ofNullable(byKey.get(user.value() + session + key));
        }
        public Optional<RolePlayTurnStoreRecord> findByTurnId(UserKey user, String session, String turn) {
            return Optional.ofNullable(byTurn.get(user.value() + session + turn));
        }
        public List<RolePlayTurn> findBySession(UserKey user, String session) {
            return byTurn.entrySet().stream().filter(entry -> entry.getKey().startsWith(user.value() + session))
                    .map(entry -> entry.getValue().turn()).toList();
        }
        public void insert(UserKey user, RolePlayTurn turn, String key, String hash) {
            RolePlayTurnStoreRecord record = new RolePlayTurnStoreRecord(key, hash, turn);
            byTurn.put(user.value() + turn.sessionId() + turn.turnId(), record);
            byKey.put(user.value() + turn.sessionId() + key, record);
        }
        public RolePlayTurn save(UserKey user, RolePlayTurn turn, long expected) {
            RolePlayTurnStoreRecord current = findByTurnId(user, turn.sessionId(), turn.turnId()).orElseThrow();
            assertEquals(expected, current.turn().version());
            RolePlayTurnStoreRecord next = new RolePlayTurnStoreRecord(current.idempotencyKey(), current.requestHash(), turn);
            byTurn.put(user.value() + turn.sessionId() + turn.turnId(), next);
            byKey.put(user.value() + turn.sessionId() + current.idempotencyKey(), next);
            return turn;
        }
    }
}
