package cn.forever24.tutor.application.roleplay;

import cn.forever24.tutor.application.quota.DailyQuotaApplicationService;
import cn.forever24.tutor.application.quota.QuotaException;
import cn.forever24.tutor.application.quota.QuotaRequestType;
import cn.forever24.tutor.application.quota.QuotaReservation;
import cn.forever24.tutor.application.training.LessonAttemptApplicationService;
import cn.forever24.tutor.application.training.LessonAttemptMutationResult;
import cn.forever24.tutor.application.training.LessonContent;
import cn.forever24.tutor.application.training.LessonContentReader;
import cn.forever24.tutor.application.training.LessonSessionApplicationException;
import cn.forever24.tutor.application.training.LessonSessionRepository;
import cn.forever24.tutor.application.training.LessonSessionTransactionOperations;
import cn.forever24.tutor.application.training.SubmitLessonAttemptCommand;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonAttempt;
import cn.forever24.tutor.training.LessonAttemptStatus;
import cn.forever24.tutor.training.LessonSession;
import cn.forever24.tutor.training.LessonSessionStatus;
import cn.forever24.tutor.training.LessonStep;
import cn.forever24.tutor.training.RolePlayTurn;
import cn.forever24.tutor.training.RolePlayTurnStatus;
import cn.forever24.tutor.training.TaskAttemptInputType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public final class RolePlayApplicationService {
    private final LessonSessionRepository sessionRepository;
    private final LessonAttemptApplicationService attemptService;
    private final LessonContentReader contentReader;
    private final RolePlayTurnRepository turnRepository;
    private final LessonSessionTransactionOperations transactions;
    private final RolePlayResponder responder;
    private final DailyQuotaApplicationService quotaService;
    private final Clock clock;

    public RolePlayApplicationService(
            LessonSessionRepository sessionRepository,
            LessonAttemptApplicationService attemptService,
            LessonContentReader contentReader,
            RolePlayTurnRepository turnRepository,
            LessonSessionTransactionOperations transactions,
            RolePlayResponder responder,
            DailyQuotaApplicationService quotaService,
            Clock clock
    ) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository);
        this.attemptService = Objects.requireNonNull(attemptService);
        this.contentReader = Objects.requireNonNull(contentReader);
        this.turnRepository = Objects.requireNonNull(turnRepository);
        this.transactions = Objects.requireNonNull(transactions);
        this.responder = Objects.requireNonNull(responder);
        this.quotaService = Objects.requireNonNull(quotaService);
        this.clock = Objects.requireNonNull(clock);
    }

    public RolePlayStreamResult stream(
            String userKeyValue, String sessionId, RolePlayMessageCommand command, String idempotencyKey
    ) {
        UserKey userKey = new UserKey(userKeyValue);
        String sid = required(sessionId, "sessionId");
        String key = idempotencyKey(idempotencyKey);
        Objects.requireNonNull(command, "command is required");
        String requestHash = hash(command.taskId() + "|" + Objects.toString(command.text(), "") + "|"
                + Objects.toString(command.audioAssetId(), "") + "|" + command.conversationTurnId());

        var existingByKey = turnRepository.findByIdempotencyKey(userKey, sid, key);
        var existingByTurn = turnRepository.findByTurnId(userKey, sid, command.conversationTurnId());
        if (existingByTurn.isPresent() && !existingByTurn.orElseThrow().idempotencyKey().equals(key)) {
            throw LessonSessionApplicationException.idempotencyConflict("role-play turn");
        }
        RolePlayTurnStoreRecord existing = existingByKey.orElseGet(() -> existingByTurn.orElse(null));
        if (existing != null) {
            requireSameRequest(existing, requestHash);
            if (existing.turn().status() == RolePlayTurnStatus.COMPLETED) {
                return completedEvents(existing.turn(), true);
            }
            if (existing.turn().status() == RolePlayTurnStatus.FAILED_FINAL) {
                return errorEvents(existing.turn(), existing.turn().errorCode(), false, true);
            }
        }

        LessonAttemptMutationResult attemptResult = attemptService.submit(
                userKey.value(), sid,
                new SubmitLessonAttemptCommand(
                        command.taskId(), command.text() == null ? TaskAttemptInputType.AUDIO : TaskAttemptInputType.TEXT,
                        command.text(), command.audioAssetId(), null, null, null),
                "role-play-attempt:" + hash(sid + "|" + command.conversationTurnId()));
        LessonAttempt attempt = attemptResult.attempt();
        String learnerText = attempt.inputType() == TaskAttemptInputType.TEXT
                ? attempt.text() : attempt.transcriptConfirmed() ? attempt.transcript() : null;

        RolePlayTurn turn = transactions.execute(() -> acceptTurn(
                userKey, sid, command, key, requestHash, attempt, learnerText));
        boolean replayed = attemptResult.replayed() || turn.version() > 1 || turn.status() == RolePlayTurnStatus.COMPLETED;
        if (turn.status() == RolePlayTurnStatus.COMPLETED) return completedEvents(turn, true);
        if (turn.status() == RolePlayTurnStatus.FAILED_FINAL) {
            return errorEvents(turn, turn.errorCode(), false, true);
        }
        if (learnerText == null) return transcriptEvents(turn, attempt, replayed);

        LessonSession session = sessionRepository.findById(userKey, sid)
                .orElseThrow(LessonSessionApplicationException::notFound);
        LessonContent content = contentReader.read(session.resourceId(), session.resourceVersion());
        RolePlayTask task = requireBoundary(session, content, command.taskId());
        List<RolePlayHistoryTurn> completedHistory = turnRepository.findBySession(userKey, sid).stream()
                .filter(value -> value.status() == RolePlayTurnStatus.COMPLETED
                        && !value.turnId().equals(turn.turnId()))
                .map(value -> new RolePlayHistoryTurn(value.learnerText(), value.replyText()))
                .toList();
        List<RolePlayHistoryTurn> history = completedHistory.subList(
                Math.max(0, completedHistory.size() - 8), completedHistory.size());

        QuotaReservation reservation = null;
        try {
            reservation = quotaService.reserve(userKey.value(), QuotaRequestType.ROLE_PLAY_REPLY,
                    "role-play:" + hash(key + "|" + turn.version()));
            RolePlayResponse response = responder.respond(new RolePlayResponseContext(
                    sid, turn.turnId(), session.resourceId(), session.resourceVersion(),
                    session.skillUnitVariantId(), session.episodeMappingId(), task, learnerText, history));
            RolePlayTurn completed = complete(userKey, turn, response);
            quotaService.commit(reservation);
            return completedEvents(completed, replayed);
        } catch (QuotaException exception) {
            RolePlayTurn failed = fail(userKey, turn, "QUOTA_EXCEEDED", true);
            return errorEvents(failed, "QUOTA_EXCEEDED", true, replayed);
        } catch (RolePlayResponderException exception) {
            quotaService.refund(reservation);
            RolePlayTurn failed = fail(userKey, turn, exception.code(), exception.retryable());
            return errorEvents(failed, exception.code(), exception.retryable(), replayed);
        } catch (RuntimeException exception) {
            quotaService.refund(reservation);
            RolePlayTurn failed = fail(userKey, turn, "AI_TEMPORARILY_UNAVAILABLE", true);
            return errorEvents(failed, "AI_TEMPORARILY_UNAVAILABLE", true, replayed);
        }
    }

    public List<RolePlayTurn> listTurns(String userKeyValue, String sessionId) {
        UserKey userKey = new UserKey(userKeyValue);
        String sid = required(sessionId, "sessionId");
        sessionRepository.findById(userKey, sid).orElseThrow(LessonSessionApplicationException::notFound);
        return turnRepository.findBySession(userKey, sid);
    }

    private RolePlayTurn acceptTurn(
            UserKey userKey, String sessionId, RolePlayMessageCommand command, String idempotencyKey,
            String requestHash, LessonAttempt attempt, String learnerText
    ) {
        var replay = turnRepository.findByIdempotencyKey(userKey, sessionId, idempotencyKey);
        if (replay.isPresent()) {
            RolePlayTurnStoreRecord record = replay.orElseThrow();
            requireSameRequest(record, requestHash);
            return refreshTranscript(userKey, record.turn(), learnerText);
        }
        var duplicateTurn = turnRepository.findByTurnId(userKey, sessionId, command.conversationTurnId());
        if (duplicateTurn.isPresent()) {
            RolePlayTurnStoreRecord record = duplicateTurn.orElseThrow();
            requireSameRequest(record, requestHash);
            return refreshTranscript(userKey, record.turn(), learnerText);
        }
        LessonSession session = sessionRepository.findByIdForUpdate(userKey, sessionId)
                .orElseThrow(LessonSessionApplicationException::notFound);
        var lockedReplay = turnRepository.findByIdempotencyKey(userKey, sessionId, idempotencyKey);
        if (lockedReplay.isPresent()) {
            RolePlayTurnStoreRecord record = lockedReplay.orElseThrow();
            requireSameRequest(record, requestHash);
            return refreshTranscript(userKey, record.turn(), learnerText);
        }
        var lockedDuplicateTurn = turnRepository.findByTurnId(userKey, sessionId, command.conversationTurnId());
        if (lockedDuplicateTurn.isPresent()) {
            RolePlayTurnStoreRecord record = lockedDuplicateTurn.orElseThrow();
            if (!record.idempotencyKey().equals(idempotencyKey)) {
                throw LessonSessionApplicationException.idempotencyConflict("role-play turn");
            }
            requireSameRequest(record, requestHash);
            return refreshTranscript(userKey, record.turn(), learnerText);
        }
        LessonContent content = contentReader.read(session.resourceId(), session.resourceVersion());
        requireBoundary(session, content, command.taskId());
        RolePlayTurn accepted = RolePlayTurn.accepted(
                command.conversationTurnId(), sessionId, attempt.attemptId(), command.taskId(), learnerText,
                learnerText == null, clock.instant());
        turnRepository.insert(userKey, accepted, idempotencyKey, requestHash);
        return accepted;
    }

    private RolePlayTurn refreshTranscript(UserKey userKey, RolePlayTurn turn, String learnerText) {
        if (turn.status() == RolePlayTurnStatus.AWAITING_TRANSCRIPT && learnerText != null) {
            RolePlayTurn updated = turn.withLearnerText(learnerText);
            return turnRepository.save(userKey, updated, turn.version());
        }
        return turn;
    }

    private RolePlayTask requireBoundary(LessonSession session, LessonContent content, String taskId) {
        if (session.status() != LessonSessionStatus.IN_PROGRESS || session.currentStep() != LessonStep.ROLE_PLAY) {
            throw LessonSessionApplicationException.stateConflict("lesson session is not accepting role-play turns");
        }
        RolePlayTask task = content.rolePlayTask();
        if (task == null || !task.taskId().equals(taskId)) {
            throw LessonSessionApplicationException.stateConflict("role-play task is outside the locked lesson boundary");
        }
        return task;
    }

    private RolePlayTurn complete(UserKey userKey, RolePlayTurn observedTurn, RolePlayResponse response) {
        return transactions.execute(() -> {
            RolePlayTurn current = turnRepository.findByTurnId(
                    userKey, observedTurn.sessionId(), observedTurn.turnId()).orElseThrow().turn();
            if (current.status() == RolePlayTurnStatus.COMPLETED) return current;
            RolePlayTurn completed = current.complete(
                    response.text(), response.promptVersion(), response.providerId(), response.modelId(),
                    response.traceId(), clock.instant());
            return turnRepository.save(userKey, completed, current.version());
        });
    }

    private RolePlayTurn fail(UserKey userKey, RolePlayTurn observed, String code, boolean retryable) {
        return transactions.execute(() -> {
            RolePlayTurn current = turnRepository.findByTurnId(
                    userKey, observed.sessionId(), observed.turnId()).orElseThrow().turn();
            if (current.status() == RolePlayTurnStatus.COMPLETED) return current;
            return turnRepository.save(userKey, current.fail(code, retryable), current.version());
        });
    }

    private static RolePlayStreamResult completedEvents(RolePlayTurn turn, boolean replayed) {
        List<RolePlayStreamEvent> events = new ArrayList<>();
        events.add(acceptedEvent(1, turn, replayed));
        events.add(new RolePlayStreamEvent(2, RolePlayStreamEventType.REPLY_DELTA,
                java.util.Map.of("sequence", 1, "text", turn.replyText())));
        events.add(new RolePlayStreamEvent(3, RolePlayStreamEventType.REPLY_COMPLETED,
                java.util.Map.of("turnId", turn.turnId(), "messageId", turn.turnId() + ":reply")));
        events.add(new RolePlayStreamEvent(4, RolePlayStreamEventType.ANALYSIS_PENDING,
                java.util.Map.of("attemptId", turn.attemptId())));
        return new RolePlayStreamResult(events, replayed);
    }

    private static RolePlayStreamResult errorEvents(
            RolePlayTurn turn, String code, boolean retryable, boolean replayed
    ) {
        return new RolePlayStreamResult(List.of(
                acceptedEvent(1, turn, replayed),
                new RolePlayStreamEvent(2, RolePlayStreamEventType.STREAM_ERROR,
                        java.util.Map.of("code", code, "retryable", retryable,
                                "traceId", Objects.toString(turn.traceId(), "")))), replayed);
    }

    private static RolePlayStreamResult transcriptEvents(
            RolePlayTurn turn, LessonAttempt attempt, boolean replayed
    ) {
        String code;
        boolean retryable;
        if (attempt.status() == LessonAttemptStatus.RECEIVED) {
            code = "ASR_CONFIRMATION_REQUIRED";
            retryable = true;
        } else if (attempt.status() == LessonAttemptStatus.TRANSCRIPTION_RETRYABLE) {
            code = "ASR_TEMPORARILY_UNAVAILABLE";
            retryable = true;
        } else {
            code = "ASR_FAILED";
            retryable = false;
        }
        return errorEvents(turn, code, retryable, replayed);
    }

    private static RolePlayStreamEvent acceptedEvent(long id, RolePlayTurn turn, boolean replayed) {
        return new RolePlayStreamEvent(id, RolePlayStreamEventType.TURN_ACCEPTED,
                java.util.Map.of("attemptId", turn.attemptId(), "turnId", turn.turnId(), "replayed", replayed));
    }

    private static void requireSameRequest(RolePlayTurnStoreRecord record, String requestHash) {
        if (!record.requestHash().equals(requestHash)) {
            throw LessonSessionApplicationException.idempotencyConflict("role-play turn");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }

    private static String idempotencyKey(String value) {
        String normalized = required(value, "Idempotency-Key");
        if (normalized.length() > 128) throw new IllegalArgumentException("Idempotency-Key must not exceed 128 characters");
        return normalized;
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
