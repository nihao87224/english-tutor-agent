package cn.forever24.tutor.infrastructure.roleplay;

import cn.forever24.tutor.curriculum.TrainingType;
import cn.forever24.tutor.infrastructure.training.JdbcLessonAttemptRepository;
import cn.forever24.tutor.infrastructure.training.JdbcLessonSessionRepository;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class JdbcRolePlayTurnRepositoryTest {
    private static final UserKey USER = new UserKey("usr-1");
    private static final Instant NOW = Instant.parse("2026-08-21T01:00:00Z");
    private JdbcRolePlayTurnRepository repository;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:role_play_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(new ClassPathResource("lesson-session-schema-h2.sql")).execute(dataSource);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        new JdbcLessonSessionRepository(jdbc, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC))
                .insert(USER, session(), "start", "start-hash");
        new JdbcLessonAttemptRepository(jdbc, new ObjectMapper()).insert(USER, attempt(), "attempt-idem", "hash");
        repository = new JdbcRolePlayTurnRepository(jdbc, Clock.fixed(NOW.plusSeconds(2), ZoneOffset.UTC));
    }

    @Test
    void persistsOwnerScopedIdempotentTurnAndOptimisticCompletion() {
        RolePlayTurn accepted = RolePlayTurn.accepted(
                "turn-1", "lsn-1", "att-1", "gate-role", "Gate 24?", false, NOW);
        repository.insert(USER, accepted, "idem-1", "request-hash");

        assertEquals("request-hash", repository.findByIdempotencyKey(USER, "lsn-1", "idem-1")
                .orElseThrow().requestHash());
        assertTrue(repository.findByTurnId(new UserKey("usr-2"), "lsn-1", "turn-1").isEmpty());

        RolePlayTurn completed = accepted.complete(
                "Yes, Gate 24.", "role-play-lin-muen-v1", "stub", "model", "trace", NOW.plusSeconds(1));
        repository.save(USER, completed, accepted.version());
        RolePlayTurn restored = repository.findByTurnId(USER, "lsn-1", "turn-1").orElseThrow().turn();
        assertEquals(RolePlayTurnStatus.COMPLETED, restored.status());
        assertEquals("Yes, Gate 24.", restored.replyText());
        assertEquals(1, repository.findBySession(USER, "lsn-1").size());
        assertThrows(IllegalStateException.class, () -> repository.save(USER, completed, accepted.version()));
    }

    private static LessonAttempt attempt() {
        return new LessonAttempt("att-1", "lsn-1", "gate-role", TaskAttemptInputType.TEXT,
                "Gate 24?", null, null, null, false, LessonAttemptStatus.ANALYSIS_PENDING,
                null, NOW, 1);
    }

    private static LessonSession session() {
        return LessonSession.start(
                "lsn-1", "prx-1", 1, "blk-1", "resource-1", "1.0.0", "skill.a2", "mapping-1",
                TrainingType.ROLE_PLAY, LessonInputMode.VOICE_OR_TEXT, NOW);
    }
}
