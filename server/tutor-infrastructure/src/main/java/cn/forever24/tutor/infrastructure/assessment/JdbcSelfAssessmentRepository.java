package cn.forever24.tutor.infrastructure.assessment;

import cn.forever24.tutor.application.assessment.SelfAssessmentRepository;
import cn.forever24.tutor.assessment.FourSkillSelfAssessment;
import cn.forever24.tutor.assessment.SelfAssessmentResult;
import cn.forever24.tutor.profile.UserKey;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public class JdbcSelfAssessmentRepository implements SelfAssessmentRepository {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public JdbcSelfAssessmentRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public SelfAssessmentResult save(UserKey userKey, FourSkillSelfAssessment assessment) {
        Long userId = findUserId(userKey);
        if (userId == null) {
            throw new IllegalArgumentException("profile must exist before self assessment");
        }
        String assessmentId = "self-" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        jdbcTemplate.update("""
                        INSERT INTO self_assessment
                            (self_assessment_key, user_id, listening_level, speaking_level,
                             reading_level, writing_level, answers_json, estimated_band, completed_at_utc)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                assessmentId,
                userId,
                assessment.listening().score(),
                assessment.speaking().score(),
                assessment.reading().score(),
                assessment.writing().score(),
                toAnswersJson(assessment),
                assessment.estimatedBand().name(),
                now);
        return new SelfAssessmentResult(assessmentId, assessment.estimatedBand());
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

    private String toAnswersJson(FourSkillSelfAssessment assessment) {
        return """
                {"listening":"%s","speaking":"%s","reading":"%s","writing":"%s"}
                """.formatted(
                assessment.listening().name(),
                assessment.speaking().name(),
                assessment.reading().name(),
                assessment.writing().name()).trim();
    }
}
