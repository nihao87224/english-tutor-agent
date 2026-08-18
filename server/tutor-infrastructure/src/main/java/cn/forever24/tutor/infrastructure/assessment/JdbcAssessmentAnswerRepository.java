package cn.forever24.tutor.infrastructure.assessment;

import cn.forever24.tutor.application.assessment.AssessmentAnswerRepository;
import cn.forever24.tutor.assessment.AssessmentAnswerReceipt;
import cn.forever24.tutor.assessment.ScoredObjectiveAnswer;
import cn.forever24.tutor.assessment.ScoredOpenAnswer;
import cn.forever24.tutor.profile.UserKey;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.Set;

public class JdbcAssessmentAnswerRepository implements AssessmentAnswerRepository {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public JdbcAssessmentAnswerRepository(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public AssessmentAnswerReceipt saveObjectiveAnswer(
            UserKey userKey,
            String assessmentId,
            ScoredObjectiveAnswer answer
    ) {
        AssessmentSessionRow session = findActiveSession(userKey, assessmentId);
        AssessmentAnswerReceipt existing = findExistingReceipt(session.id(), answer.itemId());
        if (existing != null) {
            return existing;
        }

        String answerId = "answer-" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        jdbcTemplate.update("""
                        INSERT INTO assessment_attempt
                            (answer_key, assessment_id, question_key, question_type, answer_json,
                             correctness, score, evaluator_confidence, hint_level,
                             duration_ms, created_at_utc, version)
                        VALUES (?, ?, ?, ?, JSON_OBJECT('option', ?), ?, ?, ?, 0, ?, ?, 0)
                        """,
                answerId,
                session.id(),
                answer.itemId(),
                answer.questionType(),
                answer.option(),
                answer.score().correctness().name(),
                answer.score().score(),
                answer.score().evaluatorConfidence(),
                answer.clientDurationMs(),
                now);
        return findExistingReceipt(session.id(), answer.itemId());
    }

    @Override
    public AssessmentAnswerReceipt saveOpenAnswer(
            UserKey userKey,
            String assessmentId,
            ScoredOpenAnswer answer
    ) {
        AssessmentSessionRow session = findActiveSession(userKey, assessmentId);
        AssessmentAnswerReceipt existing = findExistingReceipt(session.id(), answer.itemId());
        if (existing != null) {
            return existing;
        }

        String answerId = "answer-" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        jdbcTemplate.update("""
                        INSERT INTO assessment_attempt
                            (answer_key, assessment_id, question_key, question_type, answer_json,
                             correctness, score, evaluator_confidence, hint_level,
                             duration_ms, created_at_utc, version)
                        VALUES (?, ?, ?, ?,
                                JSON_OBJECT(
                                    'text', ?,
                                    'feedback', ?,
                                    'promptVersion', ?,
                                    'schemaVersion', ?
                                ),
                                ?, ?, ?, 0, ?, ?, 0)
                        """,
                answerId,
                session.id(),
                answer.itemId(),
                answer.questionType(),
                answer.text(),
                answer.evaluation().feedback(),
                answer.evaluation().promptVersion(),
                answer.evaluation().schemaVersion(),
                answer.evaluation().correctness().name(),
                answer.evaluation().score(),
                answer.evaluation().evaluatorConfidence(),
                answer.clientDurationMs(),
                now);
        return findExistingReceipt(session.id(), answer.itemId());
    }

    @Override
    public Set<String> answeredItemIds(UserKey userKey, String assessmentId) {
        AssessmentSessionRow session = findActiveSession(userKey, assessmentId);
        return Set.copyOf(jdbcTemplate.query(
                "SELECT question_key FROM assessment_attempt WHERE assessment_id = ?",
                (resultSet, rowNum) -> resultSet.getString("question_key"),
                session.id()));
    }

    private AssessmentSessionRow findActiveSession(UserKey userKey, String assessmentId) {
        if (assessmentId == null || assessmentId.isBlank()) {
            throw new IllegalArgumentException("assessmentId is required");
        }
        try {
            return jdbcTemplate.queryForObject(
                    """
                            SELECT s.id
                            FROM assessment_session s
                            JOIN app_user u ON u.id = s.user_id
                            WHERE s.assessment_key = ?
                              AND u.user_key = ?
                              AND u.status = 'ACTIVE'
                              AND s.type = 'INITIAL'
                              AND s.status IN ('IN_PROGRESS', 'PAUSED')
                            """,
                    (resultSet, rowNum) -> new AssessmentSessionRow(resultSet.getLong("id")),
                    assessmentId,
                    userKey.value());
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("active assessment session was not found");
        }
    }

    private AssessmentAnswerReceipt findExistingReceipt(long assessmentRowId, String itemId) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                            SELECT answer_key
                            FROM assessment_attempt
                            WHERE assessment_id = ? AND question_key = ?
                            """,
                    (resultSet, rowNum) -> new AssessmentAnswerReceipt(resultSet.getString("answer_key"), true),
                    assessmentRowId,
                    itemId);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private record AssessmentSessionRow(long id) {
    }
}
