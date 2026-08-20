package cn.forever24.tutor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMysqlContainerSmokeTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("english_tutor_test")
            .withUsername("english_tutor")
            .withPassword("english_tutor");

    @Test
    void cleanMysqlStartsWithFlywayMigration() {
        SpringApplication application = new SpringApplication(TutorApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(Map.of(
                "spring.profiles.active", "container-smoke",
                "spring.data.redis.repositories.enabled", "false",
                "spring.data.redis.host", "localhost",
                "spring.data.redis.port", "6379",
                "spring.flyway.clean-disabled", "true",
                "TUTOR_JWT_SIGNING_SECRET", "test-only-jwt-signing-secret-change-me-32",
                "TUTOR_SECRET_ENCRYPTION_KEY", "0123456789abcdef0123456789abcdef"
        ));
        application.addInitializers(context -> context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("testcontainers-mysql", Map.of(
                        "spring.datasource.url", MYSQL.getJdbcUrl(),
                        "spring.datasource.username", MYSQL.getUsername(),
                        "spring.datasource.password", MYSQL.getPassword()
                ))
        ));

        try (ConfigurableApplicationContext context = application.run()) {
            assertNotNull(context.getBean("flyway"));
        }
    }
}
