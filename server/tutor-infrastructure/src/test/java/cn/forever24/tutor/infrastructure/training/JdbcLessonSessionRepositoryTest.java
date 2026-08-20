package cn.forever24.tutor.infrastructure.training;

import cn.forever24.tutor.application.training.LessonSessionApplicationException;
import cn.forever24.tutor.curriculum.TrainingType;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonInputMode;
import cn.forever24.tutor.training.LessonSession;
import cn.forever24.tutor.training.LessonStep;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcLessonSessionRepositoryTest {

    private static final UserKey USER = new UserKey("usr-1");
    private static final Instant NOW = Instant.parse("2026-08-20T02:00:00Z");

    private JdbcTemplate jdbcTemplate;
    private JdbcLessonSessionRepository repository;
    private TransactionTemplate transactions;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:lesson_session_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(new ClassPathResource("lesson-session-schema-h2.sql"))
                .execute(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcLessonSessionRepository(
                jdbcTemplate, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Test
    void storesExactReferencesAndRestoresServerCurrentStepForOwnerOnly() {
        LessonSession started = session();
        repository.insert(USER, started, "start-1", "hash-1");
        LessonSession advanced = started.completeDeterministicStep(LessonStep.SCENE_CONTEXT);
        repository.save(USER, started.version(), advanced);

        LessonSession restored = repository.findById(USER, started.sessionId()).orElseThrow();

        assertEquals("1.0.0", restored.resourceVersion());
        assertEquals(LessonStep.FIRST_LISTEN, restored.currentStep());
        assertTrue(repository.findById(new UserKey("usr-2"), started.sessionId()).isEmpty());
        assertEquals("hash-1", repository.findStartForUpdate(USER, "start-1")
                .orElseThrow().requestHash());
    }

    @Test
    void rejectsOptimisticConflictWithoutOverwritingNewerState() {
        LessonSession started = session();
        repository.insert(USER, started, "start-1", "hash-1");
        repository.save(USER, started.version(), started.pause(NOW.plusSeconds(30)));

        LessonSessionApplicationException exception = assertThrows(
                LessonSessionApplicationException.class,
                () -> repository.save(USER, started.version(),
                        started.completeDeterministicStep(LessonStep.SCENE_CONTEXT)));

        assertEquals("VERSION_CONFLICT", exception.code());
    }

    @Test
    void insertRollsBackWithSurroundingStartTransaction() {
        assertThrows(IllegalStateException.class, () -> transactions.executeWithoutResult(status -> {
            repository.insert(USER, session(), "start-1", "hash-1");
            throw new IllegalStateException("force rollback");
        }));

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM training_session", Integer.class));
    }

    private static LessonSession session() {
        return LessonSession.start(
                "lsn-1", "prx-1", 1, "blk-1", "resource-1", "1.0.0",
                "skill.a2", "mapping-1", TrainingType.ROLE_PLAY,
                LessonInputMode.VOICE_OR_TEXT, NOW);
    }
}
