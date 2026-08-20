package cn.forever24.tutor.infrastructure.planning;

import cn.forever24.tutor.application.planning.LearnerPlanningSnapshot;
import cn.forever24.tutor.application.planning.LearnerSnapshotLoader;
import cn.forever24.tutor.curriculum.CefrLevel;
import cn.forever24.tutor.planning.PrescriptionSkillState;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.UserKey;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

public final class JdbcLearnerSnapshotLoader implements LearnerSnapshotLoader {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLearnerSnapshotLoader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
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
                    states);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("initial learner profile is required before planning");
        }
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
