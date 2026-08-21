package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.training.LessonSessionApplicationException;
import cn.forever24.tutor.application.training.LessonSessionRepository;
import cn.forever24.tutor.application.training.LessonSessionStartRecord;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonInputMode;
import cn.forever24.tutor.training.LessonSession;
import cn.forever24.tutor.training.LessonSessionStatus;
import cn.forever24.tutor.training.LessonStep;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

public final class JdbcLessonSessionRepository implements LessonSessionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JdbcLessonSessionRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, Clock clock) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.objectMapper = Objects.requireNonNull(objectMapper).copy().findAndRegisterModules();
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Optional<LessonSessionStartRecord> findStartForUpdate(UserKey userKey, String idempotencyKey) {
        jdbcTemplate.queryForObject(
                "SELECT id FROM app_user WHERE user_key = ? AND status = 'ACTIVE' FOR UPDATE",
                Long.class, userKey.value());
        try {
            return Optional.of(jdbcTemplate.queryForObject(
                    lessonSelect() + " AND ts.idempotency_key = ? FOR UPDATE",
                    (resultSet, rowNum) -> new LessonSessionStartRecord(
                            resultSet.getString("start_request_hash"), map(resultSet)),
                    userKey.value(), storedStartKey(idempotencyKey)));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void insert(UserKey userKey, LessonSession session, String idempotencyKey, String requestHash) {
        StartRows rows = jdbcTemplate.queryForObject(
                """
                        SELECT u.id AS user_id, lp.id AS plan_id, lt.id AS task_id,
                               lt.resource_version_id, lt.skill_unit_variant_id, lt.episode_mapping_id
                        FROM app_user u
                        JOIN learning_plan lp ON lp.user_id = u.id AND lp.plan_key = ?
                        JOIN learning_task lt ON lt.plan_id = lp.id AND lt.task_key = ?
                        WHERE u.user_key = ? AND u.status = 'ACTIVE'
                        """,
                (resultSet, rowNum) -> new StartRows(
                        resultSet.getLong("user_id"), resultSet.getLong("plan_id"),
                        resultSet.getLong("task_id"), resultSet.getLong("resource_version_id"),
                        resultSet.getLong("skill_unit_variant_id"), resultSet.getLong("episode_mapping_id")),
                session.prescriptionId(), session.blockId(), userKey.value());
        LocalDateTime now = utc(session.startedAt());
        jdbcTemplate.update(
                """
                        INSERT INTO training_session
                            (session_key, user_id, plan_id, learning_task_id, resource_version_id,
                             skill_unit_variant_id, episode_mapping_id, prescription_version,
                             idempotency_key, start_request_hash, type, mode, status,
                             current_task_key, current_step, step_state_json, started_at_utc,
                             paused_at_utc, completed_at_utc, effective_seconds, summary_json,
                             created_at_utc, updated_at_utc, version)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SCENARIO_LESSON', ?, ?, ?, ?, ?, ?,
                                NULL, NULL, 0, NULL, ?, ?, ?)
                        """,
                session.sessionId(), rows.userId(), rows.planId(), rows.taskId(), rows.resourceVersionId(),
                rows.skillVariantId(), rows.episodeMappingId(), session.prescriptionVersion(),
                storedStartKey(idempotencyKey), requestHash, session.inputMode().name(), session.status().name(),
                session.blockId(), session.currentStep().name(), stateJson(session), now, now, now,
                session.version());
    }

    @Override
    public Optional<LessonSession> findById(UserKey userKey, String sessionId) {
        try {
            return Optional.of(jdbcTemplate.queryForObject(
                    lessonSelect() + " AND ts.session_key = ?",
                    (resultSet, rowNum) -> map(resultSet), userKey.value(), sessionId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<LessonSession> findByIdForUpdate(UserKey userKey, String sessionId) {
        try {
            return Optional.of(jdbcTemplate.queryForObject(
                    lessonSelect() + " AND ts.session_key = ? FOR UPDATE",
                    (resultSet, rowNum) -> map(resultSet), userKey.value(), sessionId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public LessonSession save(UserKey userKey, long expectedVersion, LessonSession session) {
        int updated = jdbcTemplate.update(
                """
                        UPDATE training_session
                        SET status = ?, current_step = ?, step_state_json = ?,
                            paused_at_utc = ?, completed_at_utc = ?,
                            updated_at_utc = ?, version = ?
                        WHERE user_id = (SELECT id FROM app_user WHERE user_key = ? AND status = 'ACTIVE')
                          AND session_key = ? AND type = 'SCENARIO_LESSON'
                          AND version = ?
                        """,
                session.status().name(), session.currentStep().name(), stateJson(session),
                utc(session.pausedAt()), utc(session.completedAt()), utc(clock.instant()), session.version(),
                userKey.value(), session.sessionId(), expectedVersion);
        if (updated != 1) {
            if (findById(userKey, session.sessionId()).isEmpty()) {
                throw LessonSessionApplicationException.notFound();
            }
            throw LessonSessionApplicationException.versionConflict();
        }
        return session;
    }

    private String lessonSelect() {
        return """
                SELECT ts.session_key, lp.plan_key, ts.prescription_version, lt.task_key,
                       lr.resource_key, lrv.semantic_version, suv.variant_key, em.mapping_key,
                       ts.mode, ts.status, ts.current_step, ts.step_state_json,
                       ts.started_at_utc, ts.paused_at_utc, ts.completed_at_utc, ts.version,
                       ts.start_request_hash
                FROM training_session ts
                JOIN app_user u ON u.id = ts.user_id
                JOIN learning_plan lp ON lp.id = ts.plan_id
                JOIN learning_task lt ON lt.id = ts.learning_task_id
                JOIN learning_resource_version lrv ON lrv.id = ts.resource_version_id
                JOIN learning_resource lr ON lr.id = lrv.resource_id
                JOIN curriculum_skill_unit_variant suv ON suv.id = ts.skill_unit_variant_id
                JOIN episode_mapping em ON em.id = ts.episode_mapping_id
                WHERE u.user_key = ? AND u.status = 'ACTIVE' AND ts.type = 'SCENARIO_LESSON'
                """;
    }

    private LessonSession map(ResultSet resultSet) throws SQLException {
        StepState state = readState(resultSet.getString("step_state_json"));
        return new LessonSession(
                resultSet.getString("session_key"), resultSet.getString("plan_key"),
                resultSet.getInt("prescription_version"), resultSet.getString("task_key"),
                resultSet.getString("resource_key"), resultSet.getString("semantic_version"),
                resultSet.getString("variant_key"), resultSet.getString("mapping_key"),
                LessonInputMode.valueOf(resultSet.getString("mode")),
                LessonSessionStatus.valueOf(resultSet.getString("status")),
                LessonStep.valueOf(resultSet.getString("current_step")),
                state.requiredSteps(), state.completedSteps(),
                instant(resultSet.getTimestamp("started_at_utc")),
                instant(resultSet.getTimestamp("paused_at_utc")),
                instant(resultSet.getTimestamp("completed_at_utc")), resultSet.getLong("version"));
    }

    private String stateJson(LessonSession session) {
        try {
            return objectMapper.writeValueAsString(new StepState(session.requiredSteps(), session.completedSteps()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("lesson step state could not be serialized", exception);
        }
    }

    private StepState readState(String json) {
        try {
            var node = objectMapper.readTree(json);
            return objectMapper.readValue(node.isTextual() ? node.asText() : json, StepState.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("lesson step state could not be read", exception);
        }
    }

    private static LocalDateTime utc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static String storedStartKey(String idempotencyKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(idempotencyKey.getBytes(StandardCharsets.UTF_8));
            return "LESSON_START:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record StepState(List<LessonStep> requiredSteps, List<LessonStep> completedSteps) {
    }

    private record StartRows(
            long userId,
            long planId,
            long taskId,
            long resourceVersionId,
            long skillVariantId,
            long episodeMappingId
    ) {
    }
}
