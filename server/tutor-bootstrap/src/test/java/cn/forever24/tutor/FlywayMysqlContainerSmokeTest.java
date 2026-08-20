package cn.forever24.tutor;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMysqlContainerSmokeTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("english_tutor_test")
            .withUsername("english_tutor")
            .withPassword("english_tutor");

    @Test
    void cleanMysqlStartsWithFlywayMigration() {
        Flyway flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load();

        flyway.migrate();

        assertTrue(Arrays.stream(flyway.info().applied())
                .anyMatch(migration -> migration.getVersion() != null
                        && "22".equals(migration.getVersion().getVersion())));
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = 'user_collection_entitlement'",
                Integer.class));
        assertEquals(7, jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM app_permission
                WHERE code IN (
                    'RESOURCE_READ', 'RESOURCE_MANAGE', 'RESOURCE_PUBLISH',
                    'COLLECTION_READ', 'COLLECTION_MANAGE',
                    'ENTITLEMENT_READ', 'ENTITLEMENT_MANAGE'
                )
                """, Integer.class));
        assertEquals(7, jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM app_role_permission rp
                JOIN app_role r ON r.id = rp.role_id
                JOIN app_permission p ON p.id = rp.permission_id
                WHERE r.code = 'ADMIN'
                  AND p.code IN (
                    'RESOURCE_READ', 'RESOURCE_MANAGE', 'RESOURCE_PUBLISH',
                    'COLLECTION_READ', 'COLLECTION_MANAGE',
                    'ENTITLEMENT_READ', 'ENTITLEMENT_MANAGE'
                  )
                """, Integer.class));
    }
}
