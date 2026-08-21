package cn.forever24.tutor.application.training;

import cn.forever24.tutor.application.audio.*;
import cn.forever24.tutor.audio.UserAudioAsset;
import cn.forever24.tutor.profile.RawContentRetention;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.*;

public final class LessonAttemptApplicationService {
    private static final System.Logger LOGGER = System.getLogger(LessonAttemptApplicationService.class.getName());
    private static final double DEFAULT_ASR_CONFIRMATION_THRESHOLD = 0.80;
    private final LessonSessionRepository sessionRepository;
    private final LessonAttemptRepository attemptRepository;
    private final LessonContentReader contentReader;
    private final LessonSessionTransactionOperations transactions;
    private final LessonAttemptKeyGenerator keyGenerator;
    private final ObjectiveAnswerScorer answerScorer;
    private final AudioAssetRepository audioAssetRepository;
    private final PrivateAudioObjectStorage objectStorage;
    private final AudioTranscriber audioTranscriber;
    private final double confirmationThreshold;
    private final Clock clock;

    public LessonAttemptApplicationService(
            LessonSessionRepository sessionRepository, LessonAttemptRepository attemptRepository,
            LessonContentReader contentReader, LessonSessionTransactionOperations transactions,
            LessonAttemptKeyGenerator keyGenerator, ObjectiveAnswerScorer answerScorer,
            AudioAssetRepository audioAssetRepository, PrivateAudioObjectStorage objectStorage,
            AudioTranscriber audioTranscriber, double confirmationThreshold, Clock clock) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository);
        this.attemptRepository = Objects.requireNonNull(attemptRepository);
        this.contentReader = Objects.requireNonNull(contentReader);
        this.transactions = Objects.requireNonNull(transactions);
        this.keyGenerator = Objects.requireNonNull(keyGenerator);
        this.answerScorer = Objects.requireNonNull(answerScorer);
        this.audioAssetRepository = Objects.requireNonNull(audioAssetRepository);
        this.objectStorage = Objects.requireNonNull(objectStorage);
        this.audioTranscriber = Objects.requireNonNull(audioTranscriber);
        if (confirmationThreshold < 0 || confirmationThreshold > 1) {
            throw new IllegalArgumentException("ASR confirmation threshold must be between 0 and 1");
        }
        this.confirmationThreshold = confirmationThreshold;
        this.clock = Objects.requireNonNull(clock);
    }

    public static double defaultAsrConfirmationThreshold() { return DEFAULT_ASR_CONFIRMATION_THRESHOLD; }

    public LessonAttemptMutationResult submit(
            String userKeyValue, String sessionId, SubmitLessonAttemptCommand command, String idempotencyKey) {
        UserKey userKey = new UserKey(userKeyValue);
        String normalizedSessionId = required(sessionId, "sessionId");
        String normalizedKey = idempotencyKey(idempotencyKey);
        Objects.requireNonNull(command, "command is required");
        if (command.inputType() != TaskAttemptInputType.TEXT && command.inputType() != TaskAttemptInputType.AUDIO) {
            throw new IllegalArgumentException("lesson attempts support TEXT or AUDIO input");
        }
        String requestHash = hash(command);
        LessonAttemptMutationResult stored = transactions.execute(() -> storeInitialAttempt(
                userKey, normalizedSessionId, command, normalizedKey, requestHash));
        if (command.inputType() != TaskAttemptInputType.AUDIO || stored.replayed()) return stored;
        return transcribe(userKey, normalizedSessionId, stored.attempt());
    }

    public LessonAttemptMutationResult confirmTranscript(
            String userKeyValue, String sessionId, String attemptId,
            ConfirmTranscriptCommand command, String idempotencyKey) {
        UserKey userKey = new UserKey(userKeyValue);
        String sid = required(sessionId, "sessionId");
        String aid = required(attemptId, "attemptId");
        String key = idempotencyKey(idempotencyKey);
        Objects.requireNonNull(command, "command is required");
        String requestHash = hash(command.decision() + "|" + Objects.toString(command.correctedText(), ""));
        return transactions.execute(() -> {
            var replay = attemptRepository.findByTranscriptConfirmationKey(userKey, sid, aid, key);
            if (replay.isPresent()) {
                if (!replay.orElseThrow().requestHash().equals(requestHash)) {
                    throw LessonSessionApplicationException.idempotencyConflict("transcript confirmation");
                }
                LessonSession session = sessionRepository.findById(userKey, sid)
                        .orElseThrow(LessonSessionApplicationException::notFound);
                return new LessonAttemptMutationResult(replay.orElseThrow().attempt(), progress(userKey, session), true);
            }
            LessonSession session = sessionRepository.findByIdForUpdate(userKey, sid)
                    .orElseThrow(LessonSessionApplicationException::notFound);
            LessonAttempt current = attemptRepository.findById(userKey, sid, aid)
                    .orElseThrow(LessonSessionApplicationException::attemptNotFound);
            if (current.inputType() != TaskAttemptInputType.AUDIO || current.transcript() == null
                    || current.transcript().isBlank() || current.transcriptConfirmed()) {
                throw LessonSessionApplicationException.transcriptConfirmationRequired();
            }
            LessonAttempt updated = switch (command.decision()) {
                case CONFIRM -> current.withTranscription(current.transcript(), current.asrConfidence(), true,
                        LessonAttemptStatus.ANALYSIS_PENDING);
                case CORRECT -> current.withTranscription(command.correctedText(), current.asrConfidence(), true,
                        LessonAttemptStatus.ANALYSIS_PENDING);
                case RE_RECORD -> current.withTranscription(current.transcript(), current.asrConfidence(), false,
                        LessonAttemptStatus.RETRY_REQUIRED);
            };
            attemptRepository.updateTranscriptConfirmation(userKey, updated, current.version(), key, requestHash);
            LessonContent content = contentReader.read(session.resourceId(), session.resourceVersion());
            List<LessonAttempt> attempts = replace(attemptRepository.findBySession(userKey, sid), updated);
            LessonSession advanced = command.decision() == TranscriptConfirmationDecision.RE_RECORD
                    ? session : advanceIfEligible(session, content, attempts);
            if (advanced != session) sessionRepository.save(userKey, session.version(), advanced);
            return new LessonAttemptMutationResult(updated, progress(advanced, content, attempts), false);
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

    private LessonAttemptMutationResult storeInitialAttempt(
            UserKey userKey, String sessionId, SubmitLessonAttemptCommand command,
            String idempotencyKey, String requestHash) {
        var replay = attemptRepository.findByIdempotencyKey(userKey, sessionId, idempotencyKey);
        if (replay.isPresent()) return replay(userKey, sessionId, requestHash, replay.orElseThrow());
        LessonSession session = sessionRepository.findByIdForUpdate(userKey, sessionId)
                .orElseThrow(LessonSessionApplicationException::notFound);
        var lockedReplay = attemptRepository.findByIdempotencyKey(userKey, sessionId, idempotencyKey);
        if (lockedReplay.isPresent()) return replay(userKey, sessionId, requestHash, lockedReplay.orElseThrow());
        LessonContent content = contentReader.read(session.resourceId(), session.resourceVersion());
        List<LessonAttempt> existing = attemptRepository.findBySession(userKey, sessionId);
        LessonAttempt attempt = createAttempt(userKey, session, content, existing, command);
        attemptRepository.insert(userKey, attempt, idempotencyKey, requestHash);
        List<LessonAttempt> includingNew = new ArrayList<>(existing);
        includingNew.add(attempt);
        LessonSession updated = command.inputType() == TaskAttemptInputType.TEXT
                ? advanceIfEligible(session, content, includingNew) : session;
        if (updated != session) sessionRepository.save(userKey, session.version(), updated);
        return new LessonAttemptMutationResult(attempt, progress(updated, content, includingNew), false);
    }

    private LessonAttemptMutationResult replay(
            UserKey userKey, String sessionId, String requestHash, LessonAttemptStoreRecord record) {
        if (!record.requestHash().equals(requestHash)) {
            throw LessonSessionApplicationException.idempotencyConflict("lesson attempt");
        }
        LessonSession session = sessionRepository.findById(userKey, sessionId)
                .orElseThrow(LessonSessionApplicationException::notFound);
        return new LessonAttemptMutationResult(record.attempt(), progress(userKey, session), true);
    }

    private LessonAttemptMutationResult transcribe(UserKey userKey, String sessionId, LessonAttempt pending) {
        UserAudioAsset asset = audioAssetRepository.findById(userKey, pending.audioAssetId())
                .filter(UserAudioAsset::ready).orElseThrow(LessonSessionApplicationException::audioAssetNotFound);
        LessonAttempt updated;
        try {
            byte[] content = objectStorage.read(asset.objectKey());
            var result = audioTranscriber.transcribe(new AudioTranscriptionRequest(
                    pending.attemptId(), content, asset.mimeType(), Duration.ofMillis(asset.durationMs())));
            boolean confirmed = result.confidence() >= confirmationThreshold;
            updated = pending.withTranscription(result.text(), result.confidence(), confirmed,
                    confirmed ? LessonAttemptStatus.ANALYSIS_PENDING : LessonAttemptStatus.RECEIVED);
        } catch (AudioTranscriptionException exception) {
            updated = pending.withTranscription(null, null, false,
                    exception.retryable() ? LessonAttemptStatus.TRANSCRIPTION_RETRYABLE
                            : LessonAttemptStatus.TRANSCRIPTION_FAILED);
        } catch (RuntimeException exception) {
            updated = pending.withTranscription(null, null, false, LessonAttemptStatus.TRANSCRIPTION_RETRYABLE);
        }
        LessonAttempt finalUpdated = updated;
        LessonAttemptMutationResult result = transactions.execute(() -> persistTranscription(
                userKey, sessionId, pending, finalUpdated));
        if (finalUpdated.transcript() != null && asset.retention() == RawContentRetention.PROCESS_ONLY) {
            try {
                objectStorage.delete(asset.objectKey());
                audioAssetRepository.markDeleted(userKey, asset.audioAssetId());
            } catch (RuntimeException ignored) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "process-only audio deletion failed for asset {0}; retention sweep will retry",
                        asset.audioAssetId());
            }
        }
        return result;
    }

    private LessonAttemptMutationResult persistTranscription(
            UserKey userKey, String sessionId, LessonAttempt pending, LessonAttempt updated) {
        LessonSession session = sessionRepository.findByIdForUpdate(userKey, sessionId)
                .orElseThrow(LessonSessionApplicationException::notFound);
        LessonAttempt current = attemptRepository.findById(userKey, sessionId, pending.attemptId())
                .orElseThrow(LessonSessionApplicationException::attemptNotFound);
        if (current.version() != pending.version()) {
            return new LessonAttemptMutationResult(current, progress(userKey, session), true);
        }
        attemptRepository.updateTranscription(userKey, updated, pending.version());
        LessonContent content = contentReader.read(session.resourceId(), session.resourceVersion());
        List<LessonAttempt> attempts = replace(attemptRepository.findBySession(userKey, sessionId), updated);
        LessonSession advanced = updated.transcriptConfirmed() ? advanceIfEligible(session, content, attempts) : session;
        if (advanced != session) sessionRepository.save(userKey, session.version(), advanced);
        return new LessonAttemptMutationResult(updated, progress(advanced, content, attempts), false);
    }

    private LessonAttemptProgress progress(UserKey userKey, LessonSession session) {
        LessonContent content = contentReader.read(session.resourceId(), session.resourceVersion());
        return progress(session, content, attemptRepository.findBySession(userKey, session.sessionId()));
    }

    private LessonAttempt createAttempt(
            UserKey userKey, LessonSession session, LessonContent content,
            List<LessonAttempt> existing, SubmitLessonAttemptCommand command) {
        if (session.status() != LessonSessionStatus.IN_PROGRESS) {
            throw LessonSessionApplicationException.stateConflict("lesson session must be IN_PROGRESS to submit an attempt");
        }
        if (command.retryOfAttemptId() != null && existing.stream()
                .noneMatch(attempt -> attempt.attemptId().equals(command.retryOfAttemptId()))) {
            throw LessonSessionApplicationException.attemptNotFound();
        }
        LessonObjectiveResult result = null;
        LessonAttemptStatus status;
        if (session.currentStep() == LessonStep.COMPREHENSION && command.inputType() == TaskAttemptInputType.TEXT) {
            ComprehensionQuestion question = content.questions().stream()
                    .filter(candidate -> candidate.questionId().equals(command.taskId())).findFirst()
                    .orElseThrow(() -> LessonSessionApplicationException.stateConflict(
                            "task does not belong to this lesson comprehension step"));
            result = answerScorer.score(command.text(), question.answer());
            status = LessonAttemptStatus.ANALYZED;
        } else if (session.currentStep() == LessonStep.GUIDED_SPEAKING) {
            boolean matches = content.guidedSpeakingTasks().stream()
                    .anyMatch(candidate -> candidate.taskId().equals(command.taskId()));
            if (!matches) throw LessonSessionApplicationException.stateConflict(
                    "task does not belong to this lesson guided speaking step");
            if (command.inputType() == TaskAttemptInputType.AUDIO) {
                audioAssetRepository.findById(userKey, command.audioAssetId()).filter(UserAudioAsset::ready)
                        .orElseThrow(LessonSessionApplicationException::audioAssetNotFound);
                status = LessonAttemptStatus.TRANSCRIPTION_PENDING;
            } else status = LessonAttemptStatus.ANALYSIS_PENDING;
        } else throw LessonSessionApplicationException.stateConflict("current lesson step does not accept this attempt");
        return new LessonAttempt(keyGenerator.nextKey(), session.sessionId(), command.taskId(), command.inputType(),
                command.text(), command.audioAssetId(), null, null, false, status, result, clock.instant(), 1);
    }

    private LessonSession advanceIfEligible(LessonSession session, LessonContent content, List<LessonAttempt> attempts) {
        if (session.currentStep() == LessonStep.COMPREHENSION) {
            var completed = completedQuestionIds(content, attempts);
            if (!content.questions().isEmpty() && completed.size() == content.questions().size()) {
                return session.completeAttemptStep(LessonStep.COMPREHENSION);
            }
        }
        if (session.currentStep() == LessonStep.GUIDED_SPEAKING && attempts.stream().anyMatch(attempt ->
                attempt.status() == LessonAttemptStatus.ANALYSIS_PENDING
                        && (attempt.inputType() != TaskAttemptInputType.AUDIO || attempt.transcriptConfirmed())
                        && content.guidedSpeakingTasks().stream().anyMatch(task -> task.taskId().equals(attempt.taskId())))) {
            return session.completeAttemptStep(LessonStep.GUIDED_SPEAKING);
        }
        return session;
    }

    private LessonAttemptProgress progress(LessonSession session, LessonContent content, List<LessonAttempt> attempts) {
        var completed = completedQuestionIds(content, attempts);
        List<String> remaining = content.questions().stream().map(ComprehensionQuestion::questionId)
                .filter(taskId -> !completed.contains(taskId)).toList();
        String pending = attempts.stream().filter(attempt -> switch (attempt.status()) {
            case RECEIVED, TRANSCRIPTION_PENDING, TRANSCRIPTION_RETRYABLE, ANALYSIS_PENDING -> true;
            default -> false;
        }).reduce((first, second) -> second).map(LessonAttempt::attemptId).orElse(null);
        return new LessonAttemptProgress(session.currentStep(), List.copyOf(completed), remaining,
                session.currentStep() != LessonStep.COMPREHENSION || remaining.isEmpty(), pending);
    }

    private static LinkedHashSet<String> completedQuestionIds(LessonContent content, List<LessonAttempt> attempts) {
        var valid = content.questions().stream().map(ComprehensionQuestion::questionId).collect(java.util.stream.Collectors.toSet());
        var completed = new LinkedHashSet<String>();
        attempts.stream().filter(attempt -> attempt.status() == LessonAttemptStatus.ANALYZED)
                .map(LessonAttempt::taskId).filter(valid::contains).forEach(completed::add);
        return completed;
    }

    private static List<LessonAttempt> replace(List<LessonAttempt> attempts, LessonAttempt updated) {
        List<LessonAttempt> values = new ArrayList<>(attempts);
        values.removeIf(value -> value.attemptId().equals(updated.attemptId()));
        values.add(updated);
        return values;
    }

    private static String hash(SubmitLessonAttemptCommand command) {
        return hash(command.taskId() + "|" + command.inputType() + "|" + command.text() + "|"
                + Objects.toString(command.audioAssetId(), "") + "|" + Objects.toString(command.retryOfAttemptId(), "")
                + "|" + Objects.toString(command.clientStartedAt(), "") + "|" + Objects.toString(command.clientDurationMs(), ""));
    }
    private static String hash(String canonical) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
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
