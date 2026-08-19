package cn.forever24.tutor.infrastructure.assessment;

import cn.forever24.tutor.application.assessment.AssessmentResultRepository;
import cn.forever24.tutor.assessment.AssessmentAttemptEvidence;
import cn.forever24.tutor.assessment.AssessmentCorrectness;
import cn.forever24.tutor.assessment.AssessmentResult;
import cn.forever24.tutor.assessment.AssessmentSkillScore;
import cn.forever24.tutor.assessment.InitialAssessmentProfileGenerator;
import cn.forever24.tutor.profile.UserKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

public class JdbcAssessmentResultRepository implements AssessmentResultRepository {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public JdbcAssessmentResultRepository(JdbcTemplate jdbcTemplate, Clock clock, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Override
    public AssessmentResult completeInitialAssessment(UserKey userKey, String assessmentId) {
        AssessmentSessionRow session = findOwnedInitialSession(userKey, assessmentId);
        if ("COMPLETED".equals(session.status()) && session.resultSummaryJson() != null) {
            return deserializeResult(session.resultSummaryJson());
        }

        List<AssessmentAttemptEvidence> attempts = findAttempts(session.id());
        if (attempts.isEmpty()) {
            throw new IllegalArgumentException("assessment has no submitted attempts");
        }

        AssessmentResult result = InitialAssessmentProfileGenerator.generate(assessmentId, attempts);
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        jdbcTemplate.update("""
                        UPDATE assessment_session
                        SET status = 'COMPLETED',
                            completed_at_utc = ?,
                            estimated_remaining_minutes = 0,
                            result_summary_json = ?,
                            confidence = ?,
                            version = version + 1
                        WHERE id = ?
                        """,
                now,
                serializeResult(result),
                result.confidence(),
                session.id());
        upsertSkillStates(session.userId(), result, now);
        return result;
    }

    @Override
    public AssessmentResult getAssessmentResult(UserKey userKey, String assessmentId) {
        AssessmentSessionRow session = findOwnedInitialSession(userKey, assessmentId);
        if (!"COMPLETED".equals(session.status()) || session.resultSummaryJson() == null) {
            throw new IllegalArgumentException("assessment result was not found");
        }
        return deserializeResult(session.resultSummaryJson());
    }

    @Override
    public boolean hasCompletedInitialAssessmentResult(UserKey userKey) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM assessment_session s
                        JOIN app_user u ON u.id = s.user_id
                        WHERE u.user_key = ?
                          AND u.status = 'ACTIVE'
                          AND s.type = 'INITIAL'
                          AND s.status = 'COMPLETED'
                          AND s.result_summary_json IS NOT NULL
                        """,
                Integer.class,
                userKey.value());
        return count != null && count > 0;
    }

    private AssessmentSessionRow findOwnedInitialSession(UserKey userKey, String assessmentId) {
        if (assessmentId == null || assessmentId.isBlank()) {
            throw new IllegalArgumentException("assessmentId is required");
        }
        try {
            return jdbcTemplate.queryForObject(
                    """
                            SELECT s.id, s.user_id, s.status, s.result_summary_json
                            FROM assessment_session s
                            JOIN app_user u ON u.id = s.user_id
                            WHERE s.assessment_key = ?
                              AND u.user_key = ?
                              AND u.status = 'ACTIVE'
                              AND s.type = 'INITIAL'
                            """,
                    (resultSet, rowNum) -> new AssessmentSessionRow(
                            resultSet.getLong("id"),
                            resultSet.getLong("user_id"),
                            resultSet.getString("status"),
                            resultSet.getString("result_summary_json")),
                    assessmentId,
                    userKey.value());
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("assessment session was not found");
        }
    }

    private List<AssessmentAttemptEvidence> findAttempts(long sessionId) {
        return jdbcTemplate.query(
                """
                        SELECT question_key, correctness, score, evaluator_confidence
                        FROM assessment_attempt
                        WHERE assessment_id = ?
                        ORDER BY id
                        """,
                (resultSet, rowNum) -> new AssessmentAttemptEvidence(
                        resultSet.getString("question_key"),
                        AssessmentCorrectness.valueOf(resultSet.getString("correctness")),
                        resultSet.getBigDecimal("score"),
                        resultSet.getBigDecimal("evaluator_confidence")),
                sessionId);
    }

    private void upsertSkillStates(long userId, AssessmentResult result, LocalDateTime now) {
        for (Map.Entry<String, AssessmentSkillScore> entry : result.skills().entrySet()) {
            AssessmentSkillScore skill = entry.getValue();
            jdbcTemplate.update("""
                            INSERT INTO learner_skill_state
                                (user_id, dimension, estimate, confidence, level, trend,
                                 evidence_count, last_evidence_at_utc, updated_at_utc, version)
                            VALUES (?, ?, ?, ?, ?, 'INITIAL', ?, ?, ?, 0)
                            ON DUPLICATE KEY UPDATE
                                estimate = VALUES(estimate),
                                confidence = VALUES(confidence),
                                level = VALUES(level),
                                trend = VALUES(trend),
                                evidence_count = VALUES(evidence_count),
                                last_evidence_at_utc = VALUES(last_evidence_at_utc),
                                updated_at_utc = VALUES(updated_at_utc),
                                version = version + 1
                            """,
                    userId,
                    entry.getKey(),
                    skill.score().divide(new BigDecimal("100.0000"), 5, RoundingMode.HALF_UP),
                    skill.confidence(),
                    skill.level(),
                    skill.evidence().size(),
                    now,
                    now);
        }
    }

    private String serializeResult(AssessmentResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("assessment result serialization failed", exception);
        }
    }

    private AssessmentResult deserializeResult(String json) {
        try {
            return objectMapper.readValue(json, AssessmentResult.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("assessment result deserialization failed", exception);
        }
    }

    private record AssessmentSessionRow(long id, long userId, String status, String resultSummaryJson) {
    }
}
