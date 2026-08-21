package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.training.EvidenceSummary;
import cn.forever24.tutor.application.training.LessonEvidenceRepository;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonAttempt;
import cn.forever24.tutor.training.LessonSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** JDBC implementation deliberately uses the locked session references, never a current catalog version. */
public final class JdbcLessonEvidenceRepository implements LessonEvidenceRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcLessonEvidenceRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.objectMapper = Objects.requireNonNull(objectMapper).copy().findAndRegisterModules();
    }

    @Override
    public EvidenceSummary record(UserKey userKey, LessonSession session, LessonAttempt attempt) {
        Existing existing = existing(userKey, attempt.attemptId());
        if (existing != null) return summary(existing.evidenceId(), attempt.attemptId());
        AttemptRow row = row(userKey, session.sessionId(), attempt.attemptId());
        List<Skill> skills = skills(row.variantId());
        if (skills.isEmpty()) throw new IllegalStateException("locked variant has no target skill");
        BigDecimal score = score(attempt);
        LocalDateTime now = LocalDateTime.ofInstant(attempt.submittedAt(), ZoneOffset.UTC);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO learning_evidence
                    (evidence_key, user_id, attempt_id, skill_unit_variant_id, resource_version_id, task_type,
                     source, evidence_type, skill_dimension, knowledge_key, result, raw_score, weight,
                     independence, transfer_level, delay_days, evaluator_confidence, metadata_json,
                     criteria_results_json, policy_version, occurred_at_utc)
                    VALUES (?, ?, ?, ?, ?, 'SPEAKING', 'SCENARIO_LESSON', 'INDEPENDENT_USE', ?, ?, ?, ?,
                            0.65000, 1.00000, 0.30000, 0, 0.80000, ?, ?, 'V2-P0-1', ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, sha256(attempt.attemptId()));
            statement.setLong(2, row.userId());
            statement.setLong(3, row.attemptDbId());
            statement.setLong(4, row.variantId());
            statement.setLong(5, row.resourceVersionId());
            statement.setString(6, skills.getFirst().skillKey());
            statement.setString(7, "scenario:" + session.resourceId() + ":" + attempt.taskId());
            statement.setString(8, score.compareTo(new BigDecimal("0.99999")) >= 0 ? "CORRECT" : "PARTIAL");
            statement.setBigDecimal(9, score);
            statement.setString(10, json(Map.of("attemptId", attempt.attemptId(), "taskId", attempt.taskId(),
                    "promptVersion", attempt.analysis().promptVersion())));
            statement.setString(11, json(attempt.analysis().criteria()));
            statement.setObject(12, now);
            return statement;
        }, keyHolder);
        Number generated = keyHolder.getKey();
        if (generated == null) throw new IllegalStateException("learning evidence id was not generated");
        long evidenceId = generated.longValue();
        for (Skill skill : skills) {
            BigDecimal before = updateSkillState(row.userId(), skill.skillKey(), score, now);
            BigDecimal after = estimateAfter(before, score);
            jdbcTemplate.update("""
                    INSERT INTO learning_evidence_skill
                    (evidence_id, skill_id, role, impact_score, previous_estimate, next_estimate)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, evidenceId, skill.id(), skill.role(), score, before, after);
        }
        return summary(evidenceId, attempt.attemptId());
    }

    private Existing existing(UserKey userKey, String attemptId) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT le.id FROM learning_evidence le
                    JOIN task_attempt ta ON ta.id = le.attempt_id
                    JOIN app_user u ON u.id = le.user_id
                    WHERE u.user_key = ? AND ta.attempt_key = ?
                    """, (rs, row) -> new Existing(rs.getLong(1)), userKey.value(), attemptId);
        } catch (EmptyResultDataAccessException ignored) { return null; }
    }

    private AttemptRow row(UserKey userKey, String sessionId, String attemptId) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT u.id AS user_id, ta.id AS attempt_id, ts.skill_unit_variant_id, ts.resource_version_id
                    FROM task_attempt ta
                    JOIN training_session ts ON ts.id = ta.session_id
                    JOIN app_user u ON u.id = ta.user_id
                    WHERE u.user_key = ? AND ts.session_key = ? AND ta.attempt_key = ?
                    """, (rs, row) -> new AttemptRow(rs.getLong("user_id"), rs.getLong("attempt_id"),
                    rs.getLong("skill_unit_variant_id"), rs.getLong("resource_version_id")),
                    userKey.value(), sessionId, attemptId);
        } catch (EmptyResultDataAccessException exception) { throw new IllegalStateException("attempt owner mismatch", exception); }
    }

    private List<Skill> skills(long variantId) {
        return jdbcTemplate.query("""
                SELECT cs.id, cs.skill_key, cvts.role
                FROM curriculum_variant_target_skill cvts
                JOIN curriculum_skill cs ON cs.id = cvts.skill_id
                WHERE cvts.variant_id = ? ORDER BY cvts.role, cs.skill_key
                """, (rs, row) -> new Skill(rs.getLong("id"), rs.getString("skill_key"), rs.getString("role")), variantId);
    }

    private BigDecimal updateSkillState(long userId, String dimension, BigDecimal score, LocalDateTime now) {
        try {
            State current = jdbcTemplate.queryForObject("""
                    SELECT estimate, confidence FROM learner_skill_state
                    WHERE user_id = ? AND dimension = ? FOR UPDATE
                    """, (rs, row) -> new State(rs.getBigDecimal("estimate"), rs.getBigDecimal("confidence")), userId, dimension);
            BigDecimal next = estimateAfter(current.estimate(), score);
            jdbcTemplate.update("""
                    UPDATE learner_skill_state SET estimate = ?, confidence = LEAST(1, confidence + 0.03000),
                    trend = CASE WHEN ? >= estimate THEN 'IMPROVING' ELSE 'STABLE' END,
                    evidence_count = evidence_count + 1, last_evidence_at_utc = ?, updated_at_utc = ?, version = version + 1
                    WHERE user_id = ? AND dimension = ?
                    """, next, score, now, now, userId, dimension);
            return current.estimate();
        } catch (EmptyResultDataAccessException ignored) {
            BigDecimal initial = new BigDecimal("0.50000");
            jdbcTemplate.update("""
                    INSERT INTO learner_skill_state
                    (user_id, dimension, estimate, confidence, level, trend, evidence_count, last_evidence_at_utc, updated_at_utc, version)
                    VALUES (?, ?, ?, 0.65000, 'B1', 'IMPROVING', 1, ?, ?, 1)
                    """, userId, dimension, estimateAfter(initial, score), now, now);
            return initial;
        }
    }

    private EvidenceSummary summary(long evidenceId, String attemptId) {
        List<String> skills = jdbcTemplate.query("""
                SELECT cs.skill_key FROM learning_evidence_skill les
                JOIN curriculum_skill cs ON cs.id = les.skill_id
                WHERE les.evidence_id = ? ORDER BY cs.skill_key
                """, (rs, row) -> rs.getString(1), evidenceId);
        String focus = "Keep practising the locked communication goal in a new situation.";
        return new EvidenceSummary(attemptId, 1, skills, focus);
    }

    private static BigDecimal score(LessonAttempt attempt) {
        long total = attempt.analysis().criteria().size();
        long met = attempt.analysis().criteria().stream().filter(value -> value.satisfied()).count();
        return BigDecimal.valueOf(met).divide(BigDecimal.valueOf(total), 5, RoundingMode.HALF_UP);
    }
    private static BigDecimal estimateAfter(BigDecimal before, BigDecimal score) {
        return before.multiply(new BigDecimal("0.80000")).add(score.multiply(new BigDecimal("0.20000")))
                .setScale(5, RoundingMode.HALF_UP);
    }
    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("evidence JSON serialization failed", exception); }
    }
    private static String sha256(String text) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
    }
    private record Existing(long evidenceId) { }
    private record AttemptRow(long userId, long attemptDbId, long variantId, long resourceVersionId) { }
    private record Skill(long id, String skillKey, String role) { }
    private record State(BigDecimal estimate, BigDecimal confidence) { }
}
