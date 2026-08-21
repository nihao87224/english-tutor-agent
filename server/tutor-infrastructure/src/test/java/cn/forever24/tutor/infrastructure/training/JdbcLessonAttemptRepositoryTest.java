package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.curriculum.TrainingType;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonAttempt;
import cn.forever24.tutor.training.LessonAttemptStatus;
import cn.forever24.tutor.training.LessonInputMode;
import cn.forever24.tutor.training.LessonObjectiveResult;
import cn.forever24.tutor.training.LessonSession;
import cn.forever24.tutor.training.TaskAttemptInputType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcLessonAttemptRepositoryTest {
    private static final UserKey USER = new UserKey("usr-1");
    private static final Instant NOW = Instant.parse("2026-08-21T01:00:00Z");
    private JdbcTemplate jdbcTemplate;
    private JdbcLessonAttemptRepository attempts;
    private TransactionTemplate transactions;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:lesson_attempt_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(new ClassPathResource("lesson-session-schema-h2.sql")).execute(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        new JdbcLessonSessionRepository(jdbcTemplate, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC))
                .insert(USER, session(), "start", "start-hash");
        attempts = new JdbcLessonAttemptRepository(jdbcTemplate, new ObjectMapper());
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Test
    void persistsObjectiveResultIdempotencyMetadataAndOwnerScope() {
        LessonAttempt attempt = objectiveAttempt();
        attempts.insert(USER, attempt, "idem-1", "request-hash");

        var restored = attempts.findById(USER, "lsn-1", "lat-1").orElseThrow();
        assertTrue(restored.objectiveResult().correct());
        assertEquals("q1", restored.taskId());
        assertEquals("request-hash", attempts.findByIdempotencyKey(USER, "lsn-1", "idem-1")
                .orElseThrow().requestHash());
        assertTrue(attempts.findById(new UserKey("usr-2"), "lsn-1", "lat-1").isEmpty());
    }

    @Test
    void rollsBackAttemptWithSurroundingLessonTransaction() {
        assertThrows(IllegalStateException.class, () -> transactions.executeWithoutResult(status -> {
            attempts.insert(USER, objectiveAttempt(), "idem-1", "request-hash");
            throw new IllegalStateException("force rollback");
        }));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_attempt", Integer.class));
    }

    private static LessonAttempt objectiveAttempt() {
        return new LessonAttempt(
                "lat-1", "lsn-1", "q1", TaskAttemptInputType.TEXT, "Gate 24",
                LessonAttemptStatus.ANALYZED, new LessonObjectiveResult(true, "Gate 24", "Answer confirmed."),
                NOW, 1);
    }

    private static LessonSession session() {
        return LessonSession.start(
                "lsn-1", "prx-1", 1, "blk-1", "resource-1", "1.0.0", "skill.a2", "mapping-1",
                TrainingType.ROLE_PLAY, LessonInputMode.VOICE_OR_TEXT, NOW);
    }
}
