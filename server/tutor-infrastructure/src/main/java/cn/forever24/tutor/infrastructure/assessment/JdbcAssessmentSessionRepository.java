package cn.forever24.tutor.infrastructure.assessment;

import cn.forever24.tutor.application.assessment.AssessmentSessionRepository;
import cn.forever24.tutor.assessment.AssessmentSession;
import cn.forever24.tutor.assessment.AssessmentSessionStatus;
import cn.forever24.tutor.profile.UserKey;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public class JdbcAssessmentSessionRepository implements AssessmentSessionRepository {

    private static final String BLUEPRINT_VERSION = "initial-blueprint-v1";
    private static final String CONTENT_VERSION = "assessment-content-v1";

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public JdbcAssessmentSessionRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public AssessmentSession startOrResumeInitialAssessment(UserKey userKey, int targetMinutes) {
        Long userId = findUserId(userKey);
        if (userId == null) {
            throw new IllegalArgumentException("profile must exist before starting assessment");
        }
        AssessmentSession existing = findActiveInitialSession(userId);
        if (existing != null) {
            return existing;
        }

        String assessmentId = "assessment-" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        jdbcTemplate.update("""
                        INSERT INTO assessment_session
                            (assessment_key, user_id, type, status, target_minutes,
                             estimated_remaining_minutes, blueprint_version, content_version,
                             started_at_utc, elapsed_seconds, version)
                        VALUES (?, ?, 'INITIAL', 'IN_PROGRESS', ?, ?, ?, ?, ?, 0, 0)
                        """,
                assessmentId,
                userId,
                targetMinutes,
                targetMinutes,
                BLUEPRINT_VERSION,
                CONTENT_VERSION,
                now);
        return findActiveInitialSession(userId);
    }

    private Long findUserId(UserKey userKey) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM app_user WHERE user_key = ? AND status = 'ACTIVE'",
                    Long.class,
                    userKey.value());
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private AssessmentSession findActiveInitialSession(long userId) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                            SELECT assessment_key, status, target_minutes, estimated_remaining_minutes
                            FROM assessment_session
                            WHERE user_id = ?
                              AND type = 'INITIAL'
                              AND status IN ('IN_PROGRESS', 'PAUSED')
                            ORDER BY id DESC
                            LIMIT 1
                            """,
                    (resultSet, rowNum) -> new AssessmentSession(
                            resultSet.getString("assessment_key"),
                            AssessmentSessionStatus.valueOf(resultSet.getString("status")),
                            resultSet.getInt("target_minutes"),
                            resultSet.getInt("estimated_remaining_minutes")),
                    userId);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }
}
