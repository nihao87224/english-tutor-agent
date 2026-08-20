package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.training.TrainingSessionCompletion;
import cn.forever24.tutor.application.training.TrainingSessionRepository;
import cn.forever24.tutor.learner.LearningEvidenceDraft;
import cn.forever24.tutor.planning.LearningPlan;
import cn.forever24.tutor.planning.LearningPlanTask;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.reporting.DailySummaryEvidence;
import cn.forever24.tutor.reporting.DailyTrainingSummary;
import cn.forever24.tutor.reporting.DailyTrainingSummaryGenerator;
import cn.forever24.tutor.training.TaskAttemptReceipt;
import cn.forever24.tutor.training.TaskAttemptResult;
import cn.forever24.tutor.training.TaskAttemptSubmission;
import cn.forever24.tutor.training.TrainingSession;
import cn.forever24.tutor.training.TrainingSessionMode;
import cn.forever24.tutor.training.TrainingSessionStatus;
import cn.forever24.tutor.training.TrainingSessionType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.HexFormat;

public class JdbcTrainingSessionRepository implements TrainingSessionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public JdbcTrainingSessionRepository(JdbcTemplate jdbcTemplate, Clock clock, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public TrainingSession startDailySession(
            UserKey userKey,
            LearningPlan plan,
            TrainingSessionMode mode,
            String idempotencyKey
    ) {
        SessionOwner owner = findOwnerAndPlan(userKey, plan.planId());
        TrainingSession existing = findByIdempotencyKey(owner.userId(), idempotencyKey);
        if (existing != null) {
            return existing;
        }
        TrainingSession session = TrainingSession.startDaily(
                "training-" + UUID.randomUUID(),
                plan.planId(),
                mode,
                plan.tasks().get(0).taskId(),
                clock.instant());
        LocalDateTime now = LocalDateTime.ofInstant(session.startedAt(), ZoneOffset.UTC);
        jdbcTemplate.update("""
                        INSERT INTO training_session
                            (session_key, user_id, plan_id, idempotency_key, type, mode, status,
                             current_task_key, started_at_utc, paused_at_utc, completed_at_utc,
                             effective_seconds, summary_json, created_at_utc, updated_at_utc, version)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, ?, NULL, ?, ?, ?)
                        """,
                session.sessionId(),
                owner.userId(),
                owner.planRowId(),
                idempotencyKey,
                session.type().name(),
                session.mode().name(),
                session.status().name(),
                session.currentTaskId(),
                now,
                session.effectiveSeconds(),
                now,
                now,
                session.version());
        return findById(userKey, session.sessionId())
                .orElseThrow(() -> new IllegalStateException("created training session was not found"));
    }

    @Override
    public Optional<TrainingSession> findById(UserKey userKey, String sessionId) {
        try {
            return Optional.of(jdbcTemplate.queryForObject(
                    """
                            SELECT ts.session_key, lp.plan_key, ts.type, ts.mode, ts.status,
                                   ts.current_task_key, ts.started_at_utc, ts.paused_at_utc,
                                   ts.completed_at_utc, ts.effective_seconds, ts.version
                            FROM training_session ts
                            JOIN app_user u ON u.id = ts.user_id
                            LEFT JOIN learning_plan lp ON lp.id = ts.plan_id
                            WHERE u.user_key = ?
                              AND u.status = 'ACTIVE'
                              AND ts.session_key = ?
                              AND ts.type <> 'SCENARIO_LESSON'
                            """,
                    (resultSet, rowNum) -> new TrainingSession(
                            resultSet.getString("session_key"),
                            resultSet.getString("plan_key"),
                            TrainingSessionType.valueOf(resultSet.getString("type")),
                            TrainingSessionMode.valueOf(resultSet.getString("mode")),
                            TrainingSessionStatus.valueOf(resultSet.getString("status")),
                            resultSet.getString("current_task_key"),
                            toInstant(resultSet.getTimestamp("started_at_utc")),
                            toInstant(resultSet.getTimestamp("paused_at_utc")),
                            toInstant(resultSet.getTimestamp("completed_at_utc")),
                            resultSet.getInt("effective_seconds"),
                            resultSet.getLong("version")),
                    userKey.value(),
                    sessionId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public TrainingSession save(UserKey userKey, TrainingSession session) {
        LocalDateTime pausedAt = toLocalDateTime(session.pausedAt());
        LocalDateTime completedAt = toLocalDateTime(session.completedAt());
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        int updated = jdbcTemplate.update("""
                        UPDATE training_session ts
                        JOIN app_user u ON u.id = ts.user_id
                        SET ts.status = ?,
                            ts.paused_at_utc = ?,
                            ts.completed_at_utc = ?,
                            ts.effective_seconds = ?,
                            ts.updated_at_utc = ?,
                            ts.version = ?
                        WHERE u.user_key = ?
                          AND u.status = 'ACTIVE'
                          AND ts.session_key = ?
                        """,
                session.status().name(),
                pausedAt,
                completedAt,
                session.effectiveSeconds(),
                now,
                session.version(),
                userKey.value(),
                session.sessionId());
        if (updated != 1) {
            throw new IllegalArgumentException("training session was not found");
        }
        return findById(userKey, session.sessionId())
                .orElseThrow(() -> new IllegalArgumentException("training session was not found"));
    }

    @Override
    @Transactional
    public TrainingSessionCompletion completeSession(UserKey userKey, TrainingSession session) {
        SessionCompletionRow row = findSessionCompletionRow(userKey, session.sessionId());
        if (row.summaryJson() != null && !row.summaryJson().isBlank()) {
            return new TrainingSessionCompletion(
                    findById(userKey, session.sessionId())
                            .orElseThrow(() -> new IllegalArgumentException("training session was not found")),
                    readSummary(row.summaryJson()));
        }
        int completedTaskCount = countAttempts(row.sessionId());
        List<DailySummaryEvidence> evidence = findSummaryEvidence(row.sessionId());
        DailyTrainingSummary summary = DailyTrainingSummaryGenerator.generate(
                session.sessionId(),
                completedTaskCount,
                evidence,
                session.completedAt() == null ? clock.instant() : session.completedAt());
        Instant completedInstant = session.completedAt() == null ? clock.instant() : session.completedAt();
        LocalDateTime completedAt = LocalDateTime.ofInstant(completedInstant, ZoneOffset.UTC);
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        jdbcTemplate.update("""
                        UPDATE training_session
                        SET status = 'COMPLETED',
                            completed_at_utc = ?,
                            summary_json = ?,
                            updated_at_utc = ?,
                            version = version + 1
                        WHERE id = ?
                        """,
                completedAt,
                summaryJson(summary),
                now,
                row.sessionId());
        return new TrainingSessionCompletion(
                findById(userKey, session.sessionId())
                        .orElseThrow(() -> new IllegalArgumentException("training session was not found")),
                summary);
    }

    @Override
    @Transactional
    public TaskAttemptReceipt submitTextAttempt(
            UserKey userKey,
            TrainingSession session,
            LearningPlanTask task,
            TaskAttemptSubmission submission,
            List<LearningEvidenceDraft> evidence,
            String idempotencyKey,
            String nextTaskId
    ) {
        List<LearningEvidenceDraft> safeEvidence = List.copyOf(evidence == null ? List.of() : evidence);
        SessionTaskRow row = findSessionTask(userKey, session.sessionId(), task.taskId());
        String attemptId = attemptId(userKey, session.sessionId(), task.taskId(), idempotencyKey);
        StoredAttempt existing = findAttemptByKey(attemptId);
        if (existing != null) {
            existing.requireSame(submission, objectMapper);
            return TaskAttemptReceipt.accepted(existing.attemptId(), existing.evidenceCount());
        }
        if (!session.currentTaskId().equals(task.taskId())) {
            throw new IllegalArgumentException("task is not the current training task");
        }
        if (findAttemptForTask(row.sessionId(), row.taskId()) != null) {
            throw new IllegalArgumentException("training task already has an accepted attempt");
        }

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        jdbcTemplate.update("""
                        INSERT INTO task_attempt
                            (attempt_key, session_id, task_id, user_id, attempt_no, input_type,
                             input_text, audio_asset_id, answer_json, hint_level, result, score,
                             submitted_at_utc, evaluator_version, created_at_utc, updated_at_utc, version)
                        VALUES (?, ?, ?, ?, 1, 'TEXT', ?, NULL, ?, ?, ?, NULL, ?, NULL, ?, ?, 0)
                        """,
                attemptId,
                row.sessionId(),
                row.taskId(),
                row.userId(),
                submission.inputText(),
                answerJson(submission),
                submission.hintLevel(),
                TaskAttemptResult.UNSCORED.name(),
                now,
                now,
                now);
        long attemptRowId = findAttemptRowId(attemptId);
        persistEvidence(row.userId(), attemptRowId, attemptId, safeEvidence, now);
        if (nextTaskId != null) {
            jdbcTemplate.update("""
                            UPDATE training_session
                            SET current_task_key = ?,
                                updated_at_utc = ?,
                                version = version + 1
                            WHERE id = ?
                            """,
                    nextTaskId,
                    now,
                    row.sessionId());
        }
        return TaskAttemptReceipt.accepted(attemptId, safeEvidence.size());
    }

    private SessionOwner findOwnerAndPlan(UserKey userKey, String planId) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                            SELECT u.id AS user_id, lp.id AS plan_id
                            FROM app_user u
                            JOIN learning_plan lp ON lp.user_id = u.id
                            WHERE u.user_key = ?
                              AND u.status = 'ACTIVE'
                              AND lp.plan_key = ?
                            """,
                    (resultSet, rowNum) -> new SessionOwner(
                            resultSet.getLong("user_id"),
                            resultSet.getLong("plan_id")),
                    userKey.value(),
                    planId);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("learning plan was not found");
        }
    }

    private TrainingSession findByIdempotencyKey(long userId, String idempotencyKey) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                            SELECT ts.session_key, lp.plan_key, ts.type, ts.mode, ts.status,
                                   ts.current_task_key, ts.started_at_utc, ts.paused_at_utc,
                                   ts.completed_at_utc, ts.effective_seconds, ts.version
                            FROM training_session ts
                            LEFT JOIN learning_plan lp ON lp.id = ts.plan_id
                            WHERE ts.user_id = ?
                              AND ts.idempotency_key = ?
                            """,
                    (resultSet, rowNum) -> new TrainingSession(
                            resultSet.getString("session_key"),
                            resultSet.getString("plan_key"),
                            TrainingSessionType.valueOf(resultSet.getString("type")),
                            TrainingSessionMode.valueOf(resultSet.getString("mode")),
                            TrainingSessionStatus.valueOf(resultSet.getString("status")),
                            resultSet.getString("current_task_key"),
                            toInstant(resultSet.getTimestamp("started_at_utc")),
                            toInstant(resultSet.getTimestamp("paused_at_utc")),
                            toInstant(resultSet.getTimestamp("completed_at_utc")),
                            resultSet.getInt("effective_seconds"),
                            resultSet.getLong("version")),
                    userId,
                    idempotencyKey);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private SessionTaskRow findSessionTask(UserKey userKey, String sessionId, String taskId) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                            SELECT ts.id AS session_id, u.id AS user_id, lt.id AS task_id
                            FROM training_session ts
                            JOIN app_user u ON u.id = ts.user_id
                            JOIN learning_plan lp ON lp.id = ts.plan_id
                            JOIN learning_task lt ON lt.plan_id = lp.id
                            WHERE u.user_key = ?
                              AND u.status = 'ACTIVE'
                              AND ts.session_key = ?
                              AND lt.task_key = ?
                            """,
                    (resultSet, rowNum) -> new SessionTaskRow(
                            resultSet.getLong("session_id"),
                            resultSet.getLong("user_id"),
                            resultSet.getLong("task_id")),
                    userKey.value(),
                    sessionId,
                    taskId);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("training task was not found");
        }
    }

    private StoredAttempt findAttemptByKey(String attemptId) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                            SELECT ta.attempt_key, ta.answer_json, ta.hint_level, COUNT(le.id) AS evidence_count
                            FROM task_attempt ta
                            LEFT JOIN learning_evidence le ON le.attempt_id = ta.id
                            WHERE ta.attempt_key = ?
                            GROUP BY ta.id, ta.attempt_key, ta.answer_json, ta.hint_level
                            """,
                    (resultSet, rowNum) -> new StoredAttempt(
                            resultSet.getString("attempt_key"),
                            resultSet.getString("answer_json"),
                            resultSet.getInt("hint_level"),
                            resultSet.getInt("evidence_count")),
                    attemptId);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private long findAttemptRowId(String attemptId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM task_attempt WHERE attempt_key = ?",
                Long.class,
                attemptId);
    }

    private void persistEvidence(
            long userId,
            long attemptRowId,
            String attemptId,
            List<LearningEvidenceDraft> evidence,
            LocalDateTime occurredAt
    ) {
        for (int index = 0; index < evidence.size(); index++) {
            LearningEvidenceDraft draft = evidence.get(index);
            String evidenceId = "evidence-" + sha256(attemptId + ":" + draft.skillDimension() + ":" + index)
                    .substring(0, 32);
            jdbcTemplate.update("""
                            INSERT INTO learning_evidence
                                (evidence_key, user_id, attempt_id, source, evidence_type,
                                 skill_dimension, knowledge_key, result, raw_score, weight,
                                 independence, transfer_level, delay_days, evaluator_confidence,
                                 metadata_json, occurred_at_utc, consumed_at_utc)
                            VALUES (?, ?, ?, 'TASK_ATTEMPT', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)
                            """,
                    evidenceId,
                    userId,
                    attemptRowId,
                    draft.evidenceType().name(),
                    draft.skillDimension(),
                    draft.knowledgeKey(),
                    draft.result().name(),
                    draft.rawScore(),
                    draft.weight(),
                    draft.independence(),
                    draft.transferLevel(),
                    draft.delayDays(),
                    draft.evaluatorConfidence(),
                    metadataJson(draft, attemptId),
                    occurredAt);
            updateSkillState(userId, draft, occurredAt);
        }
    }

    private void updateSkillState(long userId, LearningEvidenceDraft draft, LocalDateTime occurredAt) {
        SkillState existing = findSkillState(userId, draft.skillDimension());
        BigDecimal adjustedScore = draft.rawScore()
                .multiply(draft.weight())
                .multiply(draft.independence())
                .multiply(draft.evaluatorConfidence())
                .setScale(5, RoundingMode.HALF_UP);
        if (existing == null) {
            jdbcTemplate.update("""
                            INSERT INTO learner_skill_state
                                (user_id, dimension, estimate, confidence, level, trend,
                                 evidence_count, last_evidence_at_utc, updated_at_utc, version)
                            VALUES (?, ?, ?, ?, ?, 'UNKNOWN', 1, ?, ?, 0)
                            """,
                    userId,
                    draft.skillDimension(),
                    adjustedScore,
                    draft.evaluatorConfidence(),
                    levelFor(adjustedScore),
                    occurredAt,
                    occurredAt);
            return;
        }
        BigDecimal delta = adjustedScore.subtract(existing.estimate()).multiply(new BigDecimal("0.0800"));
        BigDecimal boundedDelta = clamp(delta, new BigDecimal("-0.0800"), new BigDecimal("0.0800"));
        BigDecimal newEstimate = clamp(existing.estimate().add(boundedDelta), BigDecimal.ZERO, BigDecimal.ONE)
                .setScale(5, RoundingMode.HALF_UP);
        BigDecimal newConfidence = clamp(existing.confidence().add(new BigDecimal("0.0200")), BigDecimal.ZERO, new BigDecimal("0.9500"))
                .setScale(5, RoundingMode.HALF_UP);
        jdbcTemplate.update("""
                        UPDATE learner_skill_state
                        SET estimate = ?,
                            confidence = ?,
                            level = ?,
                            trend = ?,
                            evidence_count = evidence_count + 1,
                            last_evidence_at_utc = ?,
                            updated_at_utc = ?,
                            version = version + 1
                        WHERE id = ?
                        """,
                newEstimate,
                newConfidence,
                levelFor(newEstimate),
                trend(existing.estimate(), newEstimate),
                occurredAt,
                occurredAt,
                existing.id());
    }

    private SkillState findSkillState(long userId, String skillDimension) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                            SELECT id, estimate, confidence
                            FROM learner_skill_state
                            WHERE user_id = ?
                              AND dimension = ?
                            """,
                    (resultSet, rowNum) -> new SkillState(
                            resultSet.getLong("id"),
                            resultSet.getBigDecimal("estimate"),
                            resultSet.getBigDecimal("confidence")),
                    userId,
                    skillDimension);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private String metadataJson(LearningEvidenceDraft draft, String attemptId) {
        Map<String, Object> metadata = new LinkedHashMap<>(draft.metadata());
        metadata.put("sourceRef", "training-attempt/" + attemptId);
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("learning evidence JSON serialization failed", exception);
        }
    }

    private String findAttemptForTask(long sessionId, long taskId) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                            SELECT attempt_key
                            FROM task_attempt
                            WHERE session_id = ?
                              AND task_id = ?
                            LIMIT 1
                            """,
                    String.class,
                    sessionId,
                    taskId);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private SessionCompletionRow findSessionCompletionRow(UserKey userKey, String sessionId) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                            SELECT ts.id AS session_id, ts.summary_json
                            FROM training_session ts
                            JOIN app_user u ON u.id = ts.user_id
                            WHERE u.user_key = ?
                              AND u.status = 'ACTIVE'
                              AND ts.session_key = ?
                            """,
                    (resultSet, rowNum) -> new SessionCompletionRow(
                            resultSet.getLong("session_id"),
                            resultSet.getString("summary_json")),
                    userKey.value(),
                    sessionId);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("training session was not found");
        }
    }

    private int countAttempts(long sessionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM task_attempt WHERE session_id = ?",
                Integer.class,
                sessionId);
        return count == null ? 0 : count;
    }

    private List<DailySummaryEvidence> findSummaryEvidence(long sessionId) {
        return jdbcTemplate.query(
                """
                        SELECT le.skill_dimension, le.evidence_type, le.result, le.knowledge_key
                        FROM learning_evidence le
                        JOIN task_attempt ta ON ta.id = le.attempt_id
                        WHERE ta.session_id = ?
                        ORDER BY le.skill_dimension, le.id
                        """,
                (resultSet, rowNum) -> new DailySummaryEvidence(
                        resultSet.getString("skill_dimension"),
                        resultSet.getString("evidence_type"),
                        resultSet.getString("result"),
                        resultSet.getString("knowledge_key")),
                sessionId);
    }

    private String summaryJson(DailyTrainingSummary summary) {
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("training summary JSON serialization failed", exception);
        }
    }

    private DailyTrainingSummary readSummary(String summaryJson) {
        try {
            return objectMapper.readValue(summaryJson, DailyTrainingSummary.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("training summary JSON deserialization failed", exception);
        }
    }

    private String answerJson(TaskAttemptSubmission submission) {
        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("textHash", submission.textHash());
        answer.put("rawTextStored", submission.inputText() != null);
        if (submission.clientDurationMs() != null) {
            answer.put("clientDurationMs", submission.clientDurationMs());
        }
        if (submission.clientStartedAt() != null) {
            answer.put("clientStartedAt", submission.clientStartedAt().toString());
        }
        if (submission.clientCompletedAt() != null) {
            answer.put("clientCompletedAt", submission.clientCompletedAt().toString());
        }
        try {
            return objectMapper.writeValueAsString(answer);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("task attempt JSON serialization failed", exception);
        }
    }

    private static String attemptId(UserKey userKey, String sessionId, String taskId, String idempotencyKey) {
        return "attempt-" + sha256(userKey.value() + ":" + sessionId + ":" + taskId + ":" + idempotencyKey)
                .substring(0, 32);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private record SessionOwner(long userId, long planRowId) {
    }

    private record SessionTaskRow(long sessionId, long userId, long taskId) {
    }

    private record SessionCompletionRow(long sessionId, String summaryJson) {
    }

    private static BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }

    private static String levelFor(BigDecimal estimate) {
        if (estimate.compareTo(new BigDecimal("0.4000")) < 0) {
            return "A1";
        }
        if (estimate.compareTo(new BigDecimal("0.5500")) < 0) {
            return "A2";
        }
        if (estimate.compareTo(new BigDecimal("0.7000")) < 0) {
            return "B1";
        }
        if (estimate.compareTo(new BigDecimal("0.8500")) < 0) {
            return "B2";
        }
        return "C1";
    }

    private static String trend(BigDecimal oldEstimate, BigDecimal newEstimate) {
        BigDecimal threshold = new BigDecimal("0.0050");
        if (newEstimate.subtract(oldEstimate).compareTo(threshold) > 0) {
            return "UP";
        }
        if (oldEstimate.subtract(newEstimate).compareTo(threshold) > 0) {
            return "DOWN";
        }
        return "STABLE";
    }

    private record SkillState(long id, BigDecimal estimate, BigDecimal confidence) {
    }

    private record StoredAttempt(String attemptId, String answerJson, int hintLevel, int evidenceCount) {

        private void requireSame(TaskAttemptSubmission submission, ObjectMapper objectMapper) {
            try {
                String existingHash = objectMapper.readTree(answerJson).path("textHash").asText();
                if (!existingHash.equals(submission.textHash()) || hintLevel != submission.hintLevel()) {
                    throw new IllegalArgumentException("Idempotency-Key was reused with a different request");
                }
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("task attempt JSON deserialization failed", exception);
            }
        }
    }
}
