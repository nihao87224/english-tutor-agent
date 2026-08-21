package cn.forever24.tutor.infrastructure.planning;

import cn.forever24.tutor.application.planning.LearnerPlanningSnapshot;
import cn.forever24.tutor.application.planning.LearnerMemory;
import cn.forever24.tutor.application.planning.LearnerSnapshotLoader;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.planning.PrescriptionSkillState;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.UserKey;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZoneId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class JdbcLearnerSnapshotLoader implements LearnerSnapshotLoader {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public JdbcLearnerSnapshotLoader(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public LearnerPlanningSnapshot load(UserKey userKey) {
        try {
            ProfileRow profile = jdbcTemplate.queryForObject(
                    """
                            SELECT u.id, p.primary_goal, p.timezone, p.daily_minutes, p.profile_version
                            FROM app_user u
                            JOIN user_learning_profile p ON p.user_id = u.id
                            WHERE u.user_key = ?
                              AND u.status = 'ACTIVE'
                              AND p.onboarding_status IN ('RESULT', 'COMPLETE')
                            """,
                    (resultSet, rowNum) -> new ProfileRow(
                            resultSet.getLong("id"),
                            PrimaryGoal.valueOf(resultSet.getString("primary_goal")),
                            ZoneId.of(resultSet.getString("timezone")),
                            resultSet.getInt("daily_minutes"),
                            resultSet.getLong("profile_version")),
                    userKey.value());
            List<PrescriptionSkillState> states = jdbcTemplate.query(
                    """
                            SELECT dimension, estimate, confidence, level, evidence_count, last_evidence_at_utc
                            FROM learner_skill_state
                            WHERE user_id = ?
                            ORDER BY estimate ASC, dimension ASC
                            """,
                    (resultSet, rowNum) -> new PrescriptionSkillState(
                            resultSet.getString("dimension"),
                            resultSet.getBigDecimal("estimate"),
                            resultSet.getBigDecimal("confidence"),
                            CefrLevel.valueOf(resultSet.getString("level")),
                            resultSet.getInt("evidence_count"),
                            resultSet.getTimestamp("last_evidence_at_utc").toInstant()),
                    profile.userId());
            if (states.isEmpty()) {
                throw new IllegalArgumentException("initial learner skill state is required before planning");
            }
            CefrLevel currentLevel = states.getFirst().level();
            return new LearnerPlanningSnapshot(
                    userKey,
                    profile.primaryGoal(),
                    profile.timezone(),
                    profile.dailyMinutes(),
                    profile.profileVersion(),
                    currentLevel,
                    states,
                    learnerMemory(profile.userId()));
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("initial learner profile is required before planning");
        }
    }

    private LearnerMemory learnerMemory(long userId) {
        Instant now = clock.instant();
        List<LearnerMemory.WeakPoint> weakPoints = jdbcTemplate.query("""
                SELECT lem.error_tag, cs.skill_key, lem.frequency, lem.severity, lem.last_occurred_at_utc
                FROM learner_error_memory lem JOIN curriculum_skill cs ON cs.id = lem.related_skill_id
                WHERE lem.user_id = ? AND lem.status = 'ACTIVE'
                ORDER BY lem.frequency DESC, lem.last_occurred_at_utc DESC, lem.error_tag ASC
                """, (rs, ignored) -> new LearnerMemory.WeakPoint(rs.getString("error_tag"),
                rs.getString("skill_key"), rs.getInt("frequency"), rs.getString("severity"),
                rs.getTimestamp("last_occurred_at_utc").toInstant()), userId);
        List<LearnerMemory.Expression> expressions = jdbcTemplate.query("""
                SELECT normalized_expression, state, confidence, last_used_at_utc
                FROM learner_expression_memory WHERE user_id = ?
                ORDER BY confidence ASC, last_used_at_utc ASC, normalized_expression ASC
                """, (rs, ignored) -> new LearnerMemory.Expression(rs.getString("normalized_expression"),
                cn.forever24.tutor.training.LearningMemoryPolicy.ExpressionState.valueOf(rs.getString("state")),
                cn.forever24.tutor.training.LearningMemoryPolicy.decayedConfidence(rs.getBigDecimal("confidence"),
                        rs.getTimestamp("last_used_at_utc").toInstant(), now), rs.getTimestamp("last_used_at_utc").toInstant()), userId);
        List<LearnerMemory.DueReview> dueReviews = jdbcTemplate.query("""
                SELECT lrs.target_type, COALESCE(cs.skill_key, lem.normalized_expression) AS target_key,
                       lrs.due_at_utc, lrs.forgetting_risk
                FROM learner_review_state lrs
                LEFT JOIN curriculum_skill cs ON cs.id = lrs.skill_id
                LEFT JOIN learner_expression_memory lem ON lem.id = lrs.expression_memory_id
                WHERE lrs.user_id = ? AND lrs.status = 'ACTIVE' AND lrs.due_at_utc <= ?
                ORDER BY lrs.due_at_utc ASC, lrs.forgetting_risk DESC
                """, (rs, ignored) -> new LearnerMemory.DueReview(rs.getString("target_type"),
                rs.getString("target_key"), rs.getTimestamp("due_at_utc").toInstant(),
                rs.getBigDecimal("forgetting_risk")), userId, java.time.LocalDateTime.ofInstant(now, java.time.ZoneOffset.UTC));
        return new LearnerMemory(weakPoints, expressions, dueReviews);
    }

    private record ProfileRow(
            long userId,
            PrimaryGoal primaryGoal,
            ZoneId timezone,
            int dailyMinutes,
            long profileVersion
    ) {
    }
}
