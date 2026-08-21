package cn.forever24.tutor.application.training;

import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonAttempt;
import cn.forever24.tutor.training.LessonAttemptStatus;
import cn.forever24.tutor.training.LessonObjectiveResult;
import cn.forever24.tutor.training.LessonSession;
import cn.forever24.tutor.training.LessonStep;
import cn.forever24.tutor.training.ObjectiveAnswerScorer;
import cn.forever24.tutor.training.TaskAttemptInputType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public final class LessonAttemptApplicationService {

    private final LessonSessionRepository sessionRepository;
    private final LessonAttemptRepository attemptRepository;
    private final LessonContentReader contentReader;
    private final LessonSessionTransactionOperations transactions;
    private final LessonAttemptKeyGenerator keyGenerator;
    private final ObjectiveAnswerScorer answerScorer;
    private final Clock clock;

    public LessonAttemptApplicationService(
            LessonSessionRepository sessionRepository,
            LessonAttemptRepository attemptRepository,
            LessonContentReader contentReader,
            LessonSessionTransactionOperations transactions,
            LessonAttemptKeyGenerator keyGenerator,
            ObjectiveAnswerScorer answerScorer,
            Clock clock
    ) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository);
        this.attemptRepository = Objects.requireNonNull(attemptRepository);
        this.contentReader = Objects.requireNonNull(contentReader);
        this.transactions = Objects.requireNonNull(transactions);
        this.keyGenerator = Objects.requireNonNull(keyGenerator);
        this.answerScorer = Objects.requireNonNull(answerScorer);
        this.clock = Objects.requireNonNull(clock);
    }

    public LessonAttemptMutationResult submit(
            String userKeyValue,
            String sessionId,
            SubmitLessonAttemptCommand command,
            String idempotencyKey
    ) {
        UserKey userKey = new UserKey(userKeyValue);
        String normalizedSessionId = required(sessionId, "sessionId");
        String normalizedKey = idempotencyKey(idempotencyKey);
        Objects.requireNonNull(command, "command is required");
        if (command.inputType() != TaskAttemptInputType.TEXT) {
            throw new IllegalArgumentException("V2-T12 accepts TEXT attempts only");
        }
        String requestHash = hash(command);

        return transactions.execute(() -> {
            var replay = attemptRepository.findByIdempotencyKey(userKey, normalizedSessionId, normalizedKey);
            if (replay.isPresent()) {
                LessonAttemptStoreRecord record = replay.orElseThrow();
                if (!record.requestHash().equals(requestHash)) {
                    throw LessonSessionApplicationException.idempotencyConflict("lesson attempt");
                }
                LessonSession replaySession = sessionRepository.findById(userKey, normalizedSessionId)
                        .orElseThrow(LessonSessionApplicationException::notFound);
                return new LessonAttemptMutationResult(record.attempt(), progress(userKey, replaySession), true);
            }

            LessonSession session = sessionRepository.findByIdForUpdate(userKey, normalizedSessionId)
                    .orElseThrow(LessonSessionApplicationException::notFound);
            var serializedReplay = attemptRepository.findByIdempotencyKey(
                    userKey, normalizedSessionId, normalizedKey);
            if (serializedReplay.isPresent()) {
                LessonAttemptStoreRecord record = serializedReplay.orElseThrow();
                if (!record.requestHash().equals(requestHash)) {
                    throw LessonSessionApplicationException.idempotencyConflict("lesson attempt");
                }
                return new LessonAttemptMutationResult(record.attempt(), progress(userKey, session), true);
            }
            LessonContent content = contentReader.read(session.resourceId(), session.resourceVersion());
            List<LessonAttempt> existing = attemptRepository.findBySession(userKey, normalizedSessionId);
            LessonAttempt attempt = createAttempt(session, content, existing, command);
            attemptRepository.insert(userKey, attempt, normalizedKey, requestHash);

            List<LessonAttempt> includingNew = new java.util.ArrayList<>(existing);
            includingNew.add(attempt);
            LessonSession updated = advanceIfEligible(session, content, includingNew);
            if (updated != session) {
                sessionRepository.save(userKey, session.version(), updated);
            }
            return new LessonAttemptMutationResult(attempt, progress(updated, content, includingNew), false);
        });
    }

    public LessonAttemptMutationResult get(String userKeyValue, String sessionId, String attemptId) {
        UserKey userKey = new UserKey(userKeyValue);
        LessonSession session = sessionRepository.findById(userKey, required(sessionId, "sessionId"))
                .orElseThrow(LessonSessionApplicationException::notFound);
        LessonAttempt attempt = attemptRepository.findById(userKey, session.sessionId(), required(attemptId, "attemptId"))
                .orElseThrow(LessonSessionApplicationException::attemptNotFound);
        return new LessonAttemptMutationResult(attempt, progress(userKey, session), false);
    }

    public LessonAttemptProgress progress(String userKeyValue, LessonSession session) {
        return progress(new UserKey(userKeyValue), session);
    }

    private LessonAttemptProgress progress(UserKey userKey, LessonSession session) {
        LessonContent content = contentReader.read(session.resourceId(), session.resourceVersion());
        return progress(session, content, attemptRepository.findBySession(userKey, session.sessionId()));
    }

    private LessonAttempt createAttempt(
            LessonSession session,
            LessonContent content,
            List<LessonAttempt> existing,
            SubmitLessonAttemptCommand command
    ) {
        if (session.status() != cn.forever24.tutor.training.LessonSessionStatus.IN_PROGRESS) {
            throw LessonSessionApplicationException.stateConflict("lesson session must be IN_PROGRESS to submit an attempt");
        }
        if (command.retryOfAttemptId() != null && existing.stream()
                .noneMatch(attempt -> attempt.attemptId().equals(command.retryOfAttemptId()))) {
            throw LessonSessionApplicationException.attemptNotFound();
        }
        LessonObjectiveResult result = null;
        LessonAttemptStatus status;
        if (session.currentStep() == LessonStep.COMPREHENSION) {
            ComprehensionQuestion question = content.questions().stream()
                    .filter(candidate -> candidate.questionId().equals(command.taskId()))
                    .findFirst()
                    .orElseThrow(() -> LessonSessionApplicationException.stateConflict(
                            "task does not belong to this lesson comprehension step"));
            result = answerScorer.score(command.text(), question.answer());
            status = LessonAttemptStatus.ANALYZED;
        } else if (session.currentStep() == LessonStep.GUIDED_SPEAKING) {
            boolean matches = content.guidedSpeakingTasks().stream()
                    .anyMatch(candidate -> candidate.taskId().equals(command.taskId()));
            if (!matches) {
                throw LessonSessionApplicationException.stateConflict(
                        "task does not belong to this lesson guided speaking step");
            }
            status = LessonAttemptStatus.ANALYSIS_PENDING;
        } else {
            throw LessonSessionApplicationException.stateConflict(
                    "current lesson step does not accept a text attempt");
        }
        return new LessonAttempt(
                keyGenerator.nextKey(), session.sessionId(), command.taskId(), command.inputType(), command.text(),
                status, result, clock.instant(), 1);
    }

    private LessonSession advanceIfEligible(
            LessonSession session,
            LessonContent content,
            List<LessonAttempt> attempts
    ) {
        if (session.currentStep() == LessonStep.COMPREHENSION) {
            var completed = completedQuestionIds(content, attempts);
            if (!content.questions().isEmpty() && completed.size() == content.questions().size()) {
                return session.completeAttemptStep(LessonStep.COMPREHENSION);
            }
        }
        if (session.currentStep() == LessonStep.GUIDED_SPEAKING && attempts.stream().anyMatch(attempt ->
                attempt.status() == LessonAttemptStatus.ANALYSIS_PENDING
                        && content.guidedSpeakingTasks().stream().anyMatch(task -> task.taskId().equals(attempt.taskId())))) {
            return session.completeAttemptStep(LessonStep.GUIDED_SPEAKING);
        }
        return session;
    }

    private LessonAttemptProgress progress(
            LessonSession session,
            LessonContent content,
            List<LessonAttempt> attempts
    ) {
        var completed = completedQuestionIds(content, attempts);
        List<String> remaining = content.questions().stream()
                .map(ComprehensionQuestion::questionId)
                .filter(taskId -> !completed.contains(taskId))
                .toList();
        String pending = attempts.stream()
                .filter(attempt -> attempt.status() == LessonAttemptStatus.ANALYSIS_PENDING)
                .reduce((first, second) -> second)
                .map(LessonAttempt::attemptId)
                .orElse(null);
        return new LessonAttemptProgress(
                session.currentStep(), List.copyOf(completed), remaining,
                session.currentStep() != LessonStep.COMPREHENSION || remaining.isEmpty(), pending);
    }

    private static LinkedHashSet<String> completedQuestionIds(
            LessonContent content,
            List<LessonAttempt> attempts
    ) {
        var validQuestionIds = content.questions().stream()
                .map(ComprehensionQuestion::questionId)
                .collect(java.util.stream.Collectors.toSet());
        var completed = new LinkedHashSet<String>();
        attempts.stream()
                .filter(attempt -> attempt.status() == LessonAttemptStatus.ANALYZED)
                .map(LessonAttempt::taskId)
                .filter(validQuestionIds::contains)
                .forEach(completed::add);
        return completed;
    }

    private static String hash(SubmitLessonAttemptCommand command) {
        String canonical = command.taskId() + "|" + command.inputType() + "|" + command.text()
                + "|" + Objects.toString(command.retryOfAttemptId(), "")
                + "|" + Objects.toString(command.clientStartedAt(), "")
                + "|" + Objects.toString(command.clientDurationMs(), "");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
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
}
