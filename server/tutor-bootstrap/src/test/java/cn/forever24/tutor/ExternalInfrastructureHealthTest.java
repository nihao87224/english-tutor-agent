package cn.forever24.tutor;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "management.endpoint.health.show-details=always",
        "spring.data.redis.repositories.enabled=false"
})
@ActiveProfiles("external-infra")
@EnabledIfEnvironmentVariable(named = "TUTOR_RUN_EXTERNAL_INFRA_TESTS", matches = "true")
class ExternalInfrastructureHealthTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private Flyway flyway;

    @Autowired
    private HealthEndpoint healthEndpoint;

    @Test
    void flywayMysqlRedisAndHealthAreUp() {
        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertEquals(1, one);

        assertTrue(Arrays.stream(flyway.info().applied())
                .anyMatch(migration -> "8".equals(migration.getVersion().getVersion())));

        assertEquals("PONG", redisTemplate.getConnectionFactory().getConnection().ping());
        assertEquals(Status.UP, healthEndpoint.health().getStatus());
    }
}
