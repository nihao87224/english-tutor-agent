package cn.forever24.tutor.infrastructure.roleplay;

import cn.forever24.tutor.application.roleplay.RolePlayTurnRepository;
import cn.forever24.tutor.application.roleplay.RolePlayTurnStoreRecord;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.RolePlayTurn;
import cn.forever24.tutor.training.RolePlayTurnStatus;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JdbcRolePlayTurnRepository implements RolePlayTurnRepository {
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public JdbcRolePlayTurnRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Optional<RolePlayTurnStoreRecord> findByIdempotencyKey(UserKey userKey, String sessionId, String key) {
        return one(select() + " AND ts.session_key = ? AND rpt.idempotency_key = ?",
                userKey.value(), sessionId, key);
    }

    @Override
    public Optional<RolePlayTurnStoreRecord> findByTurnId(UserKey userKey, String sessionId, String turnId) {
        return one(select() + " AND ts.session_key = ? AND rpt.turn_key = ?",
                userKey.value(), sessionId, turnId);
    }

    @Override
    public List<RolePlayTurn> findBySession(UserKey userKey, String sessionId) {
        return jdbcTemplate.query(select() + " AND ts.session_key = ? ORDER BY rpt.accepted_at_utc, rpt.id",
                (resultSet, rowNum) -> map(resultSet).turn(), userKey.value(), sessionId);
    }

    @Override
    public void insert(UserKey userKey, RolePlayTurn turn, String idempotencyKey, String requestHash) {
        Rows rows = jdbcTemplate.queryForObject("""
                SELECT ts.id AS session_id, ta.id AS attempt_id, ts.user_id
                FROM training_session ts
                JOIN app_user u ON u.id = ts.user_id
                JOIN task_attempt ta ON ta.session_id = ts.id AND ta.attempt_key = ? AND ta.user_id = ts.user_id
                WHERE u.user_key = ? AND u.status = 'ACTIVE' AND ts.session_key = ?
                  AND ts.type = 'SCENARIO_LESSON'
                """, (resultSet, rowNum) -> new Rows(
                resultSet.getLong("session_id"), resultSet.getLong("attempt_id"), resultSet.getLong("user_id")),
                turn.attemptId(), userKey.value(), turn.sessionId());
        jdbcTemplate.update("""
                INSERT INTO role_play_turn
                    (turn_key, session_id, attempt_id, user_id, task_key, learner_text, reply_text,
                     status, idempotency_key, request_hash, prompt_version, provider_id, model_id,
                     trace_id, error_code, accepted_at_utc, completed_at_utc, created_at_utc,
                     updated_at_utc, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, NULL, NULL, ?, NULL, ?, ?, ?)
                """, turn.turnId(), rows.sessionId(), rows.attemptId(), rows.userId(), turn.taskId(),
                turn.learnerText(), turn.replyText(), turn.status().name(), idempotencyKey, requestHash,
                utc(turn.acceptedAt()), utc(turn.acceptedAt()), utc(turn.acceptedAt()), turn.version());
    }

    @Override
    public RolePlayTurn save(UserKey userKey, RolePlayTurn turn, long expectedVersion) {
        int changed = jdbcTemplate.update("""
                UPDATE role_play_turn
                SET learner_text = ?, reply_text = ?, status = ?, prompt_version = ?, provider_id = ?,
                    model_id = ?, trace_id = ?, error_code = ?, completed_at_utc = ?,
                    updated_at_utc = ?, version = ?
                WHERE user_id = (SELECT id FROM app_user WHERE user_key = ? AND status = 'ACTIVE')
                  AND session_id = (SELECT id FROM training_session WHERE session_key = ?)
                  AND turn_key = ? AND version = ?
                """, turn.learnerText(), turn.replyText(), turn.status().name(), turn.promptVersion(),
                turn.providerId(), turn.modelId(), turn.traceId(), turn.errorCode(), utc(turn.completedAt()),
                utc(clock.instant()), turn.version(),
                userKey.value(), turn.sessionId(), turn.turnId(), expectedVersion);
        if (changed != 1) throw new IllegalStateException("role-play turn version conflict");
        return turn;
    }

    private Optional<RolePlayTurnStoreRecord> one(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, (resultSet, rowNum) -> map(resultSet), args));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    private String select() {
        return """
                SELECT rpt.turn_key, ts.session_key, ta.attempt_key, rpt.task_key, rpt.learner_text,
                       rpt.reply_text, rpt.status, rpt.idempotency_key, rpt.request_hash,
                       rpt.prompt_version, rpt.provider_id, rpt.model_id, rpt.trace_id, rpt.error_code,
                       rpt.accepted_at_utc, rpt.completed_at_utc, rpt.version
                FROM role_play_turn rpt
                JOIN training_session ts ON ts.id = rpt.session_id
                JOIN task_attempt ta ON ta.id = rpt.attempt_id
                JOIN app_user u ON u.id = rpt.user_id
                WHERE u.user_key = ? AND u.status = 'ACTIVE'
                """;
    }

    private static RolePlayTurnStoreRecord map(ResultSet resultSet) throws SQLException {
        RolePlayTurn turn = new RolePlayTurn(
                resultSet.getString("turn_key"), resultSet.getString("session_key"),
                resultSet.getString("attempt_key"), resultSet.getString("task_key"),
                resultSet.getString("learner_text"), resultSet.getString("reply_text"),
                RolePlayTurnStatus.valueOf(resultSet.getString("status")), resultSet.getString("prompt_version"),
                resultSet.getString("provider_id"), resultSet.getString("model_id"),
                resultSet.getString("trace_id"), resultSet.getString("error_code"),
                instant(resultSet.getTimestamp("accepted_at_utc")),
                instant(resultSet.getTimestamp("completed_at_utc")), resultSet.getLong("version"));
        return new RolePlayTurnStoreRecord(
                resultSet.getString("idempotency_key"), resultSet.getString("request_hash"), turn);
    }

    private static LocalDateTime utc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record Rows(long sessionId, long attemptId, long userId) { }
}
