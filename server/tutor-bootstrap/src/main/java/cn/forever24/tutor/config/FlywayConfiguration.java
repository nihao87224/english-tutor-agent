package cn.forever24.tutor.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile("!test")
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FlywayConfiguration {

    @Bean
    public Flyway flyway(DataSource dataSource, Environment environment) {
        String[] locations = environment.getProperty(
                "spring.flyway.locations",
                String[].class,
                new String[] { "classpath:db/migration" });
        boolean baselineOnMigrate = environment.getProperty(
                "spring.flyway.baseline-on-migrate",
                Boolean.class,
                false);
        boolean cleanDisabled = environment.getProperty(
                "spring.flyway.clean-disabled",
                Boolean.class,
                true);

        FluentConfiguration configuration = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .cleanDisabled(cleanDisabled);

        Flyway flyway = configuration.load();
        flyway.migrate();
        return flyway;
    }
}
