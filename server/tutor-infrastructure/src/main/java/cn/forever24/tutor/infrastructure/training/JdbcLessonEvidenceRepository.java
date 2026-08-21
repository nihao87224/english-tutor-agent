package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.training.EvidenceSummary;
import cn.forever24.tutor.application.training.LessonEvidenceRepository;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.planning.policy.SpacingPolicy;
import cn.forever24.tutor.training.AttemptCorrection;
import cn.forever24.tutor.training.LessonAttempt;
import cn.forever24.tutor.training.LessonSession;
import cn.forever24.tutor.training.LearningMemoryPolicy;
import cn.forever24.tutor.training.LearningMemoryPolicy.ExpressionState;
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
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** JDBC implementation deliberately uses the locked session references, never a current catalog version. */
public final class JdbcLessonEvidenceRepository implements LessonEvidenceRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final SpacingPolicy spacingPolicy;

    public JdbcLessonEvidenceRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, Clock clock) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.objectMapper = Objects.requireNonNull(objectMapper).copy().findAndRegisterModules();
        this.spacingPolicy = new SpacingPolicy(Objects.requireNonNull(clock));
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
        List<SkillState> skillStates = new java.util.ArrayList<>();
        for (Skill skill : skills) {
            SkillState state = updateSkillState(row.userId(), skill.skillKey(), score, now);
            skillStates.add(state);
            jdbcTemplate.update("""
                    INSERT INTO learning_evidence_skill
                    (evidence_id, skill_id, role, impact_score, previous_estimate, next_estimate)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, evidenceId, skill.id(), skill.role(), score, state.previousEstimate(), state.nextEstimate());
        }
        recordLearnerMemory(row, attempt, skills, skillStates, score, now);
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

    private SkillState updateSkillState(long userId, String dimension, BigDecimal score, LocalDateTime now) {
        try {
            State current = jdbcTemplate.queryForObject("""
                    SELECT estimate, confidence FROM learner_skill_state
                    WHERE user_id = ? AND dimension = ? FOR UPDATE
                    """, (rs, row) -> new State(rs.getBigDecimal("estimate"), rs.getBigDecimal("confidence")), userId, dimension);
            BigDecimal next = estimateAfter(current.estimate(), score);
            BigDecimal confidence = current.confidence().add(new BigDecimal("0.03000")).min(BigDecimal.ONE)
                    .setScale(5, RoundingMode.HALF_UP);
            jdbcTemplate.update("""
                    UPDATE learner_skill_state SET estimate = ?, confidence = LEAST(1, confidence + 0.03000),
                    trend = CASE WHEN ? >= estimate THEN 'IMPROVING' ELSE 'STABLE' END,
                    evidence_count = evidence_count + 1, last_evidence_at_utc = ?, updated_at_utc = ?, version = version + 1
                    WHERE user_id = ? AND dimension = ?
                    """, next, score, now, now, userId, dimension);
            return new SkillState(current.estimate(), next, confidence);
        } catch (EmptyResultDataAccessException ignored) {
            BigDecimal initial = new BigDecimal("0.50000");
            jdbcTemplate.update("""
                    INSERT INTO learner_skill_state
                    (user_id, dimension, estimate, confidence, level, trend, evidence_count, last_evidence_at_utc, updated_at_utc, version)
                    VALUES (?, ?, ?, 0.65000, 'B1', 'IMPROVING', 1, ?, ?, 1)
                    """, userId, dimension, estimateAfter(initial, score), now, now);
            return new SkillState(initial, estimateAfter(initial, score), new BigDecimal("0.65000"));
        }
    }

    /** No source text, correction suggestion, transcript or provider payload enters these projections. */
    private void recordLearnerMemory(AttemptRow row, LessonAttempt attempt, List<Skill> skills,
                                     List<SkillState> skillStates, BigDecimal score, LocalDateTime now) {
        List<Skill> targetSkills = skills.stream().filter(skill -> "TARGET".equals(skill.role())).toList();
        List<Skill> memorySkills = targetSkills.isEmpty() ? skills : targetSkills;
        for (AttemptCorrection correction : attempt.analysis().corrections()) {
            for (Skill skill : memorySkills) {
                upsertErrorMemory(row.userId(), row.attemptDbId(), skill, correction, now);
            }
        }
        for (int index = 0; index < skills.size(); index++) {
            upsertReview(row.userId(), "SKILL", skills.get(index).id(), null,
                    skillStates.get(index).confidence(), score, attempt.submittedAt());
        }
        for (String expression : attempt.analysis().naturalExpressions().stream().distinct().toList()) {
            Expression expressionMemory = upsertExpressionMemory(row.userId(), row.attemptDbId(), expression, score, now);
            upsertReview(row.userId(), "EXPRESSION", null, expressionMemory.id(), expressionMemory.confidence(),
                    score, attempt.submittedAt());
        }
    }

    private void upsertErrorMemory(long userId, long attemptId, Skill skill, AttemptCorrection correction, LocalDateTime now) {
        String errorTag = correction.category().strip().toLowerCase(java.util.Locale.ROOT);
        String severity = correction.critical() ? "HIGH" : "MEDIUM";
        jdbcTemplate.update("""
                INSERT INTO learner_error_memory
                (error_key, user_id, error_tag, related_skill_id, frequency, severity, last_attempt_id,
                 last_occurred_at_utc, status, metadata_json, version)
                VALUES (?, ?, ?, ?, 1, ?, ?, ?, 'ACTIVE', JSON_OBJECT('retention', 'CATEGORY_ONLY'), 1)
                ON DUPLICATE KEY UPDATE frequency = frequency + 1,
                    severity = CASE WHEN severity = 'HIGH' OR VALUES(severity) = 'HIGH' THEN 'HIGH' ELSE 'MEDIUM' END,
                    last_attempt_id = VALUES(last_attempt_id), last_occurred_at_utc = VALUES(last_occurred_at_utc),
                    status = 'ACTIVE', version = version + 1
                """, sha256(userId + "|" + errorTag + "|" + skill.id()), userId, errorTag, skill.id(), severity,
                attemptId, now);
    }

    private Expression upsertExpressionMemory(long userId, long attemptId, String sourceExpression,
                                               BigDecimal score, LocalDateTime now) {
        String normalized = LearningMemoryPolicy.normalizeExpression(sourceExpression);
        try {
            Expression existing = jdbcTemplate.queryForObject("""
                    SELECT id, state, confidence FROM learner_expression_memory
                    WHERE user_id = ? AND normalized_expression = ? FOR UPDATE
                    """, (rs, ignored) -> new Expression(rs.getLong("id"),
                    ExpressionState.valueOf(rs.getString("state")), rs.getBigDecimal("confidence")), userId, normalized);
            ExpressionState nextState = LearningMemoryPolicy.nextExpressionState(existing.state(), score);
            BigDecimal nextConfidence = LearningMemoryPolicy.nextConfidence(existing.confidence(), score);
            jdbcTemplate.update("""
                    UPDATE learner_expression_memory SET state = ?, confidence = ?, last_attempt_id = ?,
                    last_used_at_utc = ?, version = version + 1 WHERE id = ?
                    """, nextState.name(), nextConfidence, attemptId, now, existing.id());
            return new Expression(existing.id(), nextState, nextConfidence);
        } catch (EmptyResultDataAccessException ignored) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            BigDecimal confidence = LearningMemoryPolicy.nextConfidence(null, score);
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO learner_expression_memory
                        (expression_key, user_id, normalized_expression, state, confidence, last_attempt_id,
                         last_used_at_utc, metadata_json, version)
                        VALUES (?, ?, ?, 'PROMPTED', ?, ?, ?, JSON_OBJECT('retention', 'NORMALIZED_EXPRESSION_ONLY'), 1)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, sha256(userId + "|" + normalized));
                statement.setLong(2, userId);
                statement.setString(3, normalized);
                statement.setBigDecimal(4, confidence);
                statement.setLong(5, attemptId);
                statement.setObject(6, now);
                return statement;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) throw new IllegalStateException("expression memory id was not generated");
            return new Expression(key.longValue(), ExpressionState.PROMPTED, confidence);
        }
    }

    private void upsertReview(long userId, String targetType, Long skillId, Long expressionId,
                              BigDecimal confidence, BigDecimal score, java.time.Instant reviewedAt) {
        String targetKey = skillId == null ? "EXPRESSION|" + expressionId : "SKILL|" + skillId;
        Review existing = findReview(userId, targetType, skillId, expressionId);
        int reviewCount = existing == null ? 0 : existing.reviewCount();
        SpacingPolicy.RecallQuality quality = LearningMemoryPolicy.recallQuality(score);
        SpacingPolicy.Decision decision = spacingPolicy.evaluate(new SpacingPolicy.Input(reviewedAt, quality, confidence, reviewCount));
        if (existing == null) {
            jdbcTemplate.update("""
                    INSERT INTO learner_review_state
                    (review_key, user_id, target_type, skill_id, expression_memory_id, due_at_utc, forgetting_risk,
                     last_recall_quality, review_count, policy_version, status, version)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, 'ACTIVE', 1)
                    """, sha256(userId + "|" + targetKey), userId, targetType, skillId, expressionId,
                    LocalDateTime.ofInstant(decision.dueAt(), ZoneOffset.UTC), decision.forgettingRisk(), quality.name(),
                    decision.policyVersion().value());
            return;
        }
        jdbcTemplate.update("""
                UPDATE learner_review_state SET due_at_utc = ?, forgetting_risk = ?, last_recall_quality = ?,
                review_count = ?, policy_version = ?, status = 'ACTIVE', version = version + 1 WHERE id = ?
                """, LocalDateTime.ofInstant(decision.dueAt(), ZoneOffset.UTC), decision.forgettingRisk(), quality.name(),
                reviewCount + 1, decision.policyVersion().value(), existing.id());
    }

    private Review findReview(long userId, String targetType, Long skillId, Long expressionId) {
        try {
            String sql = skillId == null ? """
                    SELECT id, review_count FROM learner_review_state
                    WHERE user_id = ? AND target_type = ? AND expression_memory_id = ? FOR UPDATE
                    """ : """
                    SELECT id, review_count FROM learner_review_state
                    WHERE user_id = ? AND target_type = ? AND skill_id = ? FOR UPDATE
                    """;
            long targetId = skillId == null ? expressionId : skillId;
            return jdbcTemplate.queryForObject(sql, (rs, ignored) -> new Review(rs.getLong("id"), rs.getInt("review_count")),
                    userId, targetType, targetId);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
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
    private record SkillState(BigDecimal previousEstimate, BigDecimal nextEstimate, BigDecimal confidence) { }
    private record Expression(long id, ExpressionState state, BigDecimal confidence) { }
    private record Review(long id, int reviewCount) { }
}
