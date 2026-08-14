package cn.forever24.tutor.config;

import cn.forever24.tutor.application.auth.AuthApplicationService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BootstrapAdminConfiguration.BootstrapAdminProperties.class)
public class BootstrapAdminConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "tutor.auth.bootstrap-admin", name = "enabled", havingValue = "true")
    public ApplicationRunner bootstrapAdminRunner(
            AuthApplicationService authApplicationService,
            BootstrapAdminProperties properties
    ) {
        return ignored -> authApplicationService.bootstrapAdmin(properties.email(), properties.password());
    }

    @ConfigurationProperties(prefix = "tutor.auth.bootstrap-admin")
    public record BootstrapAdminProperties(boolean enabled, String email, String password) {
    }
}
