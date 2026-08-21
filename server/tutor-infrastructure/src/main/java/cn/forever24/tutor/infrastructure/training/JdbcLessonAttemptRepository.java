package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.training.LessonAttemptRepository;
import cn.forever24.tutor.application.training.LessonAttemptStoreRecord;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonAttempt;
import cn.forever24.tutor.training.LessonAttemptStatus;
import cn.forever24.tutor.training.LessonObjectiveResult;
import cn.forever24.tutor.training.AttemptAnalysis;
import cn.forever24.tutor.training.TaskAttemptInputType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class JdbcLessonAttemptRepository implements LessonAttemptRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcLessonAttemptRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.objectMapper = Objects.requireNonNull(objectMapper).copy().findAndRegisterModules();
    }

    @Override
    public Optional<LessonAttemptStoreRecord> findByIdempotencyKey(
            UserKey userKey, String sessionId, String idempotencyKey
    ) {
        try {
            return Optional.of(jdbcTemplate.queryForObject(
                    select() + " AND ts.session_key = ? AND ta.idempotency_key = ?",
                    (resultSet, rowNum) -> new LessonAttemptStoreRecord(
                            metadata(resultSet).requestHash(), map(resultSet)),
                    userKey.value(), sessionId, idempotencyKey));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<LessonAttempt> findById(UserKey userKey, String sessionId, String attemptId) {
        try {
            return Optional.of(jdbcTemplate.queryForObject(
                    select() + " AND ts.session_key = ? AND ta.attempt_key = ?",
                    (resultSet, rowNum) -> map(resultSet), userKey.value(), sessionId, attemptId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<LessonAttemptStoreRecord> findByTranscriptConfirmationKey(
            UserKey userKey, String sessionId, String attemptId, String idempotencyKey) {
        try {
            return Optional.of(jdbcTemplate.queryForObject(
                    select() + " AND ts.session_key = ? AND ta.attempt_key = ?"
                            + " AND JSON_UNQUOTE(JSON_EXTRACT(ta.answer_json, '$.confirmationKey')) = ?",
                    (resultSet, rowNum) -> new LessonAttemptStoreRecord(
                            metadata(resultSet).confirmationHash(), map(resultSet)),
                    userKey.value(), sessionId, attemptId, idempotencyKey));
        } catch (EmptyResultDataAccessException exception) { return Optional.empty(); }
    }

    @Override
    public List<LessonAttempt> findBySession(UserKey userKey, String sessionId) {
        return jdbcTemplate.query(
                select() + " AND ts.session_key = ? ORDER BY ta.submitted_at_utc, ta.id",
                (resultSet, rowNum) -> map(resultSet), userKey.value(), sessionId);
    }

    @Override
    public void insert(UserKey userKey, LessonAttempt attempt, String idempotencyKey, String requestHash) {
        AttemptRows rows = jdbcTemplate.queryForObject(
                """
                        SELECT ts.id AS session_id, ts.learning_task_id AS task_id, ts.user_id,
                               COALESCE(MAX(ta.attempt_no), 0) + 1 AS next_attempt_no
                        FROM training_session ts
                        JOIN app_user u ON u.id = ts.user_id
                        LEFT JOIN task_attempt ta ON ta.session_id = ts.id AND ta.task_id = ts.learning_task_id
                        WHERE u.user_key = ? AND ts.session_key = ? AND ts.type = 'SCENARIO_LESSON'
                        GROUP BY ts.id, ts.learning_task_id, ts.user_id
                        """,
                (resultSet, rowNum) -> new AttemptRows(
                        resultSet.getLong("session_id"), resultSet.getLong("task_id"),
                        resultSet.getLong("user_id"), resultSet.getInt("next_attempt_no")),
                userKey.value(), attempt.sessionId());
        LocalDateTime submitted = utc(attempt.submittedAt());
        Long audioAssetId = attempt.audioAssetId() == null ? null : jdbcTemplate.queryForObject(
                "SELECT id FROM user_audio_asset WHERE asset_key = ? AND user_id = ? AND status = 'READY'",
                Long.class, attempt.audioAssetId(), rows.userId());
        Long retryOfId = attempt.retryOfAttemptId() == null ? null : jdbcTemplate.queryForObject("""
                SELECT ta.id FROM task_attempt ta JOIN training_session ts ON ts.id = ta.session_id
                WHERE ts.id = ? AND ta.attempt_key = ?
                """, Long.class, rows.sessionId(), attempt.retryOfAttemptId());
        jdbcTemplate.update(
                """
                        INSERT INTO task_attempt
                            (attempt_key, idempotency_key, session_id, task_id, user_id, attempt_no,
                             input_type, input_text, audio_asset_id, answer_json, hint_level, result,
                             attempt_status, retry_of_attempt_id, evaluation_json, submitted_at_utc,
                             created_at_utc, updated_at_utc, version)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                attempt.attemptId(), idempotencyKey, rows.sessionId(), rows.taskId(), rows.userId(),
                rows.attemptNo(), attempt.inputType().name(), attempt.text(), audioAssetId,
                json(Map.of("taskId", attempt.taskId(), "requestHash", requestHash)),
                result(attempt), attempt.status().name(), retryOfId,
                attempt.objectiveResult() == null ? null : json(attempt.objectiveResult()),
                submitted, submitted, submitted, attempt.version());
    }

    @Override
    public void updateTranscription(UserKey userKey, LessonAttempt attempt, long expectedVersion) {
        int changed = jdbcTemplate.update("""
                UPDATE task_attempt ta
                JOIN training_session ts ON ts.id = ta.session_id
                JOIN app_user u ON u.id = ta.user_id
                SET ta.asr_transcript = ?, ta.asr_confidence = ?, ta.transcript_confirmed = ?,
                    ta.attempt_status = ?, ta.updated_at_utc = UTC_TIMESTAMP(6), ta.version = ?
                WHERE u.user_key = ? AND ts.session_key = ? AND ta.attempt_key = ? AND ta.version = ?
                """, attempt.transcript(), attempt.asrConfidence(), attempt.transcriptConfirmed(),
                attempt.status().name(), attempt.version(), userKey.value(), attempt.sessionId(),
                attempt.attemptId(), expectedVersion);
        if (changed != 1) throw new IllegalStateException("lesson attempt version conflict");
    }

    @Override
    public void updateTranscriptConfirmation(
            UserKey userKey, LessonAttempt attempt, long expectedVersion, String idempotencyKey, String requestHash) {
        int changed = jdbcTemplate.update("""
                UPDATE task_attempt ta
                JOIN training_session ts ON ts.id = ta.session_id
                JOIN app_user u ON u.id = ta.user_id
                SET ta.asr_transcript = ?, ta.transcript_confirmed = ?, ta.attempt_status = ?,
                    ta.answer_json = JSON_SET(ta.answer_json, '$.confirmationKey', ?, '$.confirmationHash', ?),
                    ta.updated_at_utc = UTC_TIMESTAMP(6), ta.version = ?
                WHERE u.user_key = ? AND ts.session_key = ? AND ta.attempt_key = ? AND ta.version = ?
                """, attempt.transcript(), attempt.transcriptConfirmed(), attempt.status().name(),
                idempotencyKey, requestHash, attempt.version(), userKey.value(), attempt.sessionId(),
                attempt.attemptId(), expectedVersion);
        if (changed != 1) throw new IllegalStateException("lesson attempt version conflict");
    }

    @Override
    public void updateAnalysis(UserKey userKey, LessonAttempt attempt, long expectedVersion) {
        int changed = jdbcTemplate.update("""
                UPDATE task_attempt ta
                JOIN training_session ts ON ts.id = ta.session_id
                JOIN app_user u ON u.id = ta.user_id
                SET ta.evaluation_json = ?, ta.analysis_error_code = ?, ta.evaluator_prompt_version = ?,
                    ta.provider_trace_json = ?, ta.attempt_status = ?, ta.updated_at_utc = UTC_TIMESTAMP(6), ta.version = ?
                WHERE u.user_key = ? AND ts.session_key = ? AND ta.attempt_key = ? AND ta.version = ?
                """, attempt.analysis() == null ? null : json(attempt.analysis()), attempt.analysisErrorCode(),
                attempt.analysis() == null ? null : attempt.analysis().promptVersion(),
                attempt.analysis() == null ? null : json(Map.of("providerId", attempt.analysis().providerId(),
                        "modelId", attempt.analysis().modelId(), "traceId", attempt.analysis().traceId())),
                attempt.status().name(), attempt.version(), userKey.value(), attempt.sessionId(), attempt.attemptId(), expectedVersion);
        if (changed != 1) throw new IllegalStateException("lesson attempt version conflict");
    }

    private String select() {
        return """
                SELECT ta.attempt_key, ts.session_key, ta.input_type, ta.input_text, ta.answer_json,
                       ua.asset_key AS audio_asset_key, retry.attempt_key AS retry_attempt_key, ta.asr_transcript, ta.asr_confidence,
                       ta.transcript_confirmed, ta.attempt_status, ta.evaluation_json, ta.analysis_error_code,
                       ta.submitted_at_utc, ta.version
                FROM task_attempt ta
                JOIN training_session ts ON ts.id = ta.session_id
                JOIN app_user u ON u.id = ta.user_id
                LEFT JOIN user_audio_asset ua ON ua.id = ta.audio_asset_id
                LEFT JOIN task_attempt retry ON retry.id = ta.retry_of_attempt_id
                WHERE u.user_key = ? AND u.status = 'ACTIVE' AND ts.type = 'SCENARIO_LESSON'
                """;
    }

    private LessonAttempt map(ResultSet resultSet) throws SQLException {
        Metadata metadata = metadata(resultSet);
        LessonObjectiveResult objective = null;
        AttemptAnalysis analysis = null;
        String evaluation = resultSet.getString("evaluation_json");
        if (evaluation != null && !evaluation.isBlank()) {
            try {
                var node = objectMapper.readTree(evaluation);
                if (node.has("summary") && node.has("criteria")) {
                    analysis = objectMapper.readValue(node.isTextual() ? node.asText() : evaluation, AttemptAnalysis.class);
                } else {
                    objective = objectMapper.readValue(node.isTextual() ? node.asText() : evaluation, LessonObjectiveResult.class);
                }
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("lesson attempt evaluation is invalid", exception);
            }
        }
        return new LessonAttempt(
                resultSet.getString("attempt_key"), resultSet.getString("session_key"), metadata.taskId(), resultSet.getString("retry_attempt_key"),
                TaskAttemptInputType.valueOf(resultSet.getString("input_type")), resultSet.getString("input_text"),
                resultSet.getString("audio_asset_key"), resultSet.getString("asr_transcript"),
                resultSet.getObject("asr_confidence") == null ? null : resultSet.getDouble("asr_confidence"),
                resultSet.getBoolean("transcript_confirmed"),
                LessonAttemptStatus.valueOf(resultSet.getString("attempt_status")), objective, analysis,
                resultSet.getString("analysis_error_code"),
                instant(resultSet.getTimestamp("submitted_at_utc")), resultSet.getLong("version"));
    }

    private Metadata metadata(ResultSet resultSet) throws SQLException {
        try {
            var node = objectMapper.readTree(resultSet.getString("answer_json"));
            if (node.isTextual()) node = objectMapper.readTree(node.asText());
            return new Metadata(node.path("taskId").asText(), node.path("requestHash").asText(),
                    node.path("confirmationHash").asText());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("lesson attempt metadata is invalid", exception);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("lesson attempt could not be serialized", exception);
        }
    }

    private static String result(LessonAttempt attempt) {
        if (attempt.objectiveResult() == null) return "PENDING";
        return attempt.objectiveResult().correct() ? "CORRECT" : "INCORRECT";
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp.toInstant();
    }

    private record Metadata(String taskId, String requestHash, String confirmationHash) {
    }

    private record AttemptRows(long sessionId, long taskId, long userId, int attemptNo) {
    }
}
