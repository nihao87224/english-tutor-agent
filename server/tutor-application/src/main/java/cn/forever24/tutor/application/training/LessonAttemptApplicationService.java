package cn.forever24.tutor.application.training;

import cn.forever24.tutor.application.audio.*;
import cn.forever24.tutor.audio.UserAudioAsset;
import cn.forever24.tutor.profile.RawContentRetention;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.*;
import cn.forever24.tutor.planning.policy.AttemptRetryPolicy;

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
    private final SpeakingAttemptAnalyzer speakingAttemptAnalyzer;
    private final AnalysisRetryJobRepository analysisRetryJobs;
    private final Clock clock;

    public LessonAttemptApplicationService(
            LessonSessionRepository sessionRepository, LessonAttemptRepository attemptRepository,
            LessonContentReader contentReader, LessonSessionTransactionOperations transactions,
            LessonAttemptKeyGenerator keyGenerator, ObjectiveAnswerScorer answerScorer,
            AudioAssetRepository audioAssetRepository, PrivateAudioObjectStorage objectStorage,
            AudioTranscriber audioTranscriber, double confirmationThreshold, Clock clock) {
        this(sessionRepository, attemptRepository, contentReader, transactions, keyGenerator, answerScorer,
                audioAssetRepository, objectStorage, audioTranscriber, confirmationThreshold,
                null, null, clock);
    }

    public LessonAttemptApplicationService(
            LessonSessionRepository sessionRepository, LessonAttemptRepository attemptRepository,
            LessonContentReader contentReader, LessonSessionTransactionOperations transactions,
            LessonAttemptKeyGenerator keyGenerator, ObjectiveAnswerScorer answerScorer,
            AudioAssetRepository audioAssetRepository, PrivateAudioObjectStorage objectStorage,
            AudioTranscriber audioTranscriber, double confirmationThreshold,
            SpeakingAttemptAnalyzer speakingAttemptAnalyzer, Clock clock) {
        this(sessionRepository, attemptRepository, contentReader, transactions, keyGenerator, answerScorer,
                audioAssetRepository, objectStorage, audioTranscriber, confirmationThreshold,
                speakingAttemptAnalyzer, null, clock);
    }

    public LessonAttemptApplicationService(
            LessonSessionRepository sessionRepository, LessonAttemptRepository attemptRepository,
            LessonContentReader contentReader, LessonSessionTransactionOperations transactions,
            LessonAttemptKeyGenerator keyGenerator, ObjectiveAnswerScorer answerScorer,
            AudioAssetRepository audioAssetRepository, PrivateAudioObjectStorage objectStorage,
            AudioTranscriber audioTranscriber, double confirmationThreshold,
            SpeakingAttemptAnalyzer speakingAttemptAnalyzer, AnalysisRetryJobRepository analysisRetryJobs, Clock clock) {
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
        this.speakingAttemptAnalyzer = speakingAttemptAnalyzer;
        this.analysisRetryJobs = analysisRetryJobs;
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
        if (command.inputType() != TaskAttemptInputType.AUDIO || stored.replayed()) {
            return analyzeIfConfigured(userKey, normalizedSessionId, stored);
        }
        return analyzeIfConfigured(userKey, normalizedSessionId, transcribe(userKey, normalizedSessionId, stored.attempt()));
    }

    /** Provider work is deliberately outside transactions; only the validated outcome is committed. */
    public LessonAttemptMutationResult analyzePending(String userKeyValue, String sessionId, String attemptId) {
        UserKey userKey = new UserKey(userKeyValue);
        String sid = required(sessionId, "sessionId");
        LessonSession session = sessionRepository.findById(userKey, sid)
                .orElseThrow(LessonSessionApplicationException::notFound);
        LessonAttempt attempt = attemptRepository.findById(userKey, sid, required(attemptId, "attemptId"))
                .orElseThrow(LessonSessionApplicationException::attemptNotFound);
        if (speakingAttemptAnalyzer == null || attempt.status() != LessonAttemptStatus.ANALYSIS_PENDING) {
            return new LessonAttemptMutationResult(attempt, progress(userKey, session), true);
        }
        LessonContent content = contentReader.read(session.resourceId(), session.resourceVersion());
        SpeakingAttemptAnalysisContext context = analysisContext(session, content, attempt);
        try {
            ValidatedAttemptAnalysis validated = ValidatedAttemptAnalysis.from(context, speakingAttemptAnalyzer.analyze(context));
            return transactions.execute(() -> persistAnalysis(userKey, sid, attempt, content, validated));
        } catch (SpeakingAttemptAnalysisException exception) {
            return transactions.execute(() -> persistAnalysisFailure(userKey, sid, attempt, exception));
        } catch (RuntimeException exception) {
            return transactions.execute(() -> persistAnalysisFailure(userKey, sid, attempt,
                    new SpeakingAttemptAnalysisException("AI_TEMPORARILY_UNAVAILABLE", true, "analysis provider failed")));
        }
    }

    private LessonAttemptMutationResult analyzeIfConfigured(
            UserKey userKey, String sessionId, LessonAttemptMutationResult stored) {
        if (speakingAttemptAnalyzer == null || stored.replayed()
                || stored.attempt().status() != LessonAttemptStatus.ANALYSIS_PENDING) return stored;
        LessonSession session = sessionRepository.findById(userKey, sessionId)
                .orElseThrow(LessonSessionApplicationException::notFound);
        LessonContent content = contentReader.read(session.resourceId(), session.resourceVersion());
        if (content.rolePlayTask() != null && content.rolePlayTask().taskId().equals(stored.attempt().taskId())) {
            return stored; // The durable role-play response must be stored before it is analyzed.
        }
        return analyzePending(userKey.value(), sessionId, stored.attempt().attemptId());
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
        LessonAttemptMutationResult result = transactions.execute(() -> {
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
        return analyzeIfConfigured(userKey, sid, result);
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
        LessonSession updated = session.currentStep() == LessonStep.COMPREHENSION
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
                .noneMatch(attempt -> attempt.attemptId().equals(command.retryOfAttemptId())
                        && attempt.taskId().equals(command.taskId()))) {
            throw LessonSessionApplicationException.attemptNotFound();
        }
        if (session.currentStep() == LessonStep.RETRY && command.retryOfAttemptId() == null) {
            throw LessonSessionApplicationException.stateConflict("a retry attempt must reference the original attempt");
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
        } else if (session.currentStep() == LessonStep.GUIDED_SPEAKING || session.currentStep() == LessonStep.RETRY) {
            boolean matches = content.guidedSpeakingTasks().stream()
                .anyMatch(candidate -> candidate.taskId().equals(command.taskId()));
            if (!matches && content.rolePlayTask() != null) matches = content.rolePlayTask().taskId().equals(command.taskId());
            if (!matches) throw LessonSessionApplicationException.stateConflict(
                    "task does not belong to this lesson guided speaking step");
            if (command.inputType() == TaskAttemptInputType.AUDIO) {
                audioAssetRepository.findById(userKey, command.audioAssetId()).filter(UserAudioAsset::ready)
                        .orElseThrow(LessonSessionApplicationException::audioAssetNotFound);
                status = LessonAttemptStatus.TRANSCRIPTION_PENDING;
            } else status = LessonAttemptStatus.ANALYSIS_PENDING;
        } else if (session.currentStep() == LessonStep.ROLE_PLAY && content.rolePlayTask() != null) {
            if (!content.rolePlayTask().taskId().equals(command.taskId())) {
                throw LessonSessionApplicationException.stateConflict(
                        "task does not belong to this lesson role-play step");
            }
            if (command.inputType() == TaskAttemptInputType.AUDIO) {
                audioAssetRepository.findById(userKey, command.audioAssetId()).filter(UserAudioAsset::ready)
                        .orElseThrow(LessonSessionApplicationException::audioAssetNotFound);
                status = LessonAttemptStatus.TRANSCRIPTION_PENDING;
            } else status = LessonAttemptStatus.ANALYSIS_PENDING;
        } else throw LessonSessionApplicationException.stateConflict("current lesson step does not accept this attempt");
        return new LessonAttempt(keyGenerator.nextKey(), session.sessionId(), command.taskId(), command.retryOfAttemptId(), command.inputType(),
                command.text(), command.audioAssetId(), null, null, false, status, result, null, null, clock.instant(), 1);
    }

    private LessonSession advanceIfEligible(LessonSession session, LessonContent content, List<LessonAttempt> attempts) {
        if (session.currentStep() == LessonStep.COMPREHENSION) {
            var completed = completedQuestionIds(content, attempts);
            if (!content.questions().isEmpty() && completed.size() == content.questions().size()) {
                return session.completeAttemptStep(LessonStep.COMPREHENSION);
            }
        }
        if (session.currentStep() == LessonStep.GUIDED_SPEAKING && attempts.stream().anyMatch(attempt ->
                attempt.status() == LessonAttemptStatus.ACCEPTED
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

    private LessonAttemptMutationResult persistAnalysis(
            UserKey userKey, String sessionId, LessonAttempt observed, LessonContent content,
            ValidatedAttemptAnalysis validated) {
        LessonSession session = sessionRepository.findByIdForUpdate(userKey, sessionId)
                .orElseThrow(LessonSessionApplicationException::notFound);
        LessonAttempt current = attemptRepository.findById(userKey, sessionId, observed.attemptId())
                .orElseThrow(LessonSessionApplicationException::attemptNotFound);
        if (current.version() != observed.version() || current.status() != LessonAttemptStatus.ANALYSIS_PENDING) {
            return new LessonAttemptMutationResult(current, progress(userKey, session), true);
        }
        int number = (int) attemptRepository.findBySession(userKey, sessionId).stream()
                .filter(value -> value.taskId().equals(current.taskId())).count();
        var decision = new AttemptRetryPolicy().evaluate(new AttemptRetryPolicy.Input(
                validated.failedCriteria() > 0, validated.failedCriteria(), number, 2));
        LessonAttemptStatus status = decision.action() == AttemptRetryPolicy.Action.RETRY
                ? LessonAttemptStatus.RETRY_REQUIRED : LessonAttemptStatus.ACCEPTED;
        LessonAttempt updated = current.withAnalysis(validated.analysis(), status);
        attemptRepository.updateAnalysis(userKey, updated, current.version());
        LessonSession advanced = advanceForAnalysis(session, content, current.taskId(), decision.action());
        if (advanced != session) sessionRepository.save(userKey, session.version(), advanced);
        return new LessonAttemptMutationResult(updated, progress(advanced, content,
                replace(attemptRepository.findBySession(userKey, sessionId), updated)), false);
    }

    private LessonAttemptMutationResult persistAnalysisFailure(
            UserKey userKey, String sessionId, LessonAttempt observed, SpeakingAttemptAnalysisException exception) {
        LessonSession session = sessionRepository.findByIdForUpdate(userKey, sessionId)
                .orElseThrow(LessonSessionApplicationException::notFound);
        LessonAttempt current = attemptRepository.findById(userKey, sessionId, observed.attemptId())
                .orElseThrow(LessonSessionApplicationException::attemptNotFound);
        if (current.version() != observed.version() || current.status() != LessonAttemptStatus.ANALYSIS_PENDING) {
            return new LessonAttemptMutationResult(current, progress(userKey, session), true);
        }
        LessonAttempt updated = current.withAnalysisFailure(exception.code(), exception.retryable());
        attemptRepository.updateAnalysis(userKey, updated, current.version());
        if (exception.retryable() && analysisRetryJobs != null) {
            analysisRetryJobs.schedule(userKey, sessionId, updated.attemptId(), exception.code(), clock.instant().plusSeconds(5));
        }
        return new LessonAttemptMutationResult(updated, progress(userKey, session), false);
    }

    public LessonAttemptMutationResult markAnalysisFinalFailure(String userKeyValue, String sessionId, String attemptId) {
        UserKey userKey = new UserKey(userKeyValue);
        return transactions.execute(() -> {
            LessonSession session = sessionRepository.findByIdForUpdate(userKey, sessionId)
                    .orElseThrow(LessonSessionApplicationException::notFound);
            LessonAttempt current = attemptRepository.findById(userKey, sessionId, attemptId)
                    .orElseThrow(LessonSessionApplicationException::attemptNotFound);
            if (current.status() != LessonAttemptStatus.ANALYSIS_RETRYABLE) {
                return new LessonAttemptMutationResult(current, progress(userKey, session), true);
            }
            LessonAttempt failed = current.withAnalysisFailure("ANALYSIS_RETRY_LIMIT_REACHED", false);
            attemptRepository.updateAnalysis(userKey, failed, current.version());
            return new LessonAttemptMutationResult(failed, progress(userKey, session), false);
        });
    }

    private static LessonSession advanceForAnalysis(
            LessonSession session, LessonContent content, String taskId, AttemptRetryPolicy.Action action) {
        LessonSession advanced = session;
        boolean currentAttempt = advanced.currentStep() == LessonStep.RETRY
                || advanced.currentStep() == LessonStep.GUIDED_SPEAKING && content.guidedSpeakingTasks().stream()
                .anyMatch(task -> task.taskId().equals(taskId))
                || advanced.currentStep() == LessonStep.ROLE_PLAY && content.rolePlayTask() != null
                && content.rolePlayTask().taskId().equals(taskId);
        if (currentAttempt) {
            advanced = advanced.completeAttemptStep(advanced.currentStep());
        }
        if (action == AttemptRetryPolicy.Action.RETRY && advanced.currentStep() == LessonStep.FEEDBACK) {
            return advanced.requireRetry();
        }
        return advanced;
    }

    private static SpeakingAttemptAnalysisContext analysisContext(
            LessonSession session, LessonContent content, LessonAttempt attempt) {
        GuidedSpeakingTask guided = content.guidedSpeakingTasks().stream()
                .filter(task -> task.taskId().equals(attempt.taskId())).findFirst().orElse(null);
        List<String> criteria;
        String prompt;
        if (guided != null) {
            criteria = guided.successCriteria();
            prompt = guided.prompt();
        } else if (content.rolePlayTask() != null && content.rolePlayTask().taskId().equals(attempt.taskId())) {
            criteria = content.rolePlayTask().successCriteria();
            prompt = content.rolePlayTask().goal();
        } else throw new SpeakingAttemptAnalysisException("AI_OUTPUT_INVALID", false, "attempt is outside lesson boundary");
        if (criteria.isEmpty()) throw new SpeakingAttemptAnalysisException("AI_OUTPUT_INVALID", false, "lesson has no criteria");
        List<String> keys = java.util.stream.IntStream.range(0, criteria.size())
                .mapToObj(index -> attempt.taskId() + ":criterion:" + (index + 1)).toList();
        String text = attempt.inputType() == TaskAttemptInputType.AUDIO ? attempt.transcript() : attempt.text();
        return new SpeakingAttemptAnalysisContext(attempt.attemptId(), session.sessionId(), session.resourceId(),
                session.resourceVersion(), attempt.taskId(), prompt, keys, criteria, text);
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
