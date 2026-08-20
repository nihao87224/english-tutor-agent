package cn.forever24.tutor.infrastructure.resource;

import cn.forever24.tutor.infrastructure.training.JdbcLessonSessionRepository;
import cn.forever24.tutor.curriculum.TrainingType;
import cn.forever24.tutor.profile.UserKey;
import cn.forever24.tutor.training.LessonInputMode;
import cn.forever24.tutor.training.LessonSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcHistoricalResourceAccessRepositoryTest {

    private static final UserKey OWNER = new UserKey("usr-1");
    private static final Instant NOW = Instant.parse("2026-08-20T02:00:00Z");

    private JdbcHistoricalResourceAccessRepository historicalAccess;
    private JdbcLessonSessionRepository lessonSessions;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:historical_resource_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(new ClassPathResource("lesson-session-schema-h2.sql"))
                .execute(dataSource);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        historicalAccess = new JdbcHistoricalResourceAccessRepository(jdbcTemplate);
        lessonSessions = new JdbcLessonSessionRepository(
                jdbcTemplate, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void grantsOnlyTheOwnerAccessToTheExactSessionLockedVersion() {
        LessonSession session = LessonSession.start(
                "lsn-1", "prx-1", 1, "blk-1", "resource-1", "1.0.0",
                "skill.a2", "mapping-1", TrainingType.ROLE_PLAY,
                LessonInputMode.VOICE_OR_TEXT, NOW);
        lessonSessions.insert(OWNER, session, "start-1", "hash-1");

        assertTrue(historicalAccess.hasSessionOrEvidenceReference(OWNER, "resource-1", "1.0.0"));
        assertFalse(historicalAccess.hasSessionOrEvidenceReference(OWNER, "resource-1", "2.0.0"));
        assertFalse(historicalAccess.hasSessionOrEvidenceReference(
                new UserKey("usr-2"), "resource-1", "1.0.0"));
    }
}
