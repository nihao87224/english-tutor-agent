package cn.forever24.tutor.config;

import cn.forever24.tutor.infrastructure.auth.HmacJwtAccessTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HmacJwtAccessTokenService accessTokenService) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthenticationFilter(accessTokenService), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/v1/me", "/api/v1/me/**").authenticated()
                        .requestMatchers("/api/v1/admin/**").hasAnyAuthority(
                                "DASHBOARD_READ",
                                "USER_READ",
                                "USER_UPDATE",
                                "USER_STATUS_MANAGE",
                                "USER_QUOTA_MANAGE",
                                "USER_ROLE_MANAGE",
                                "AI_PROVIDER_READ",
                                "AI_PROVIDER_MANAGE",
                                "SYSTEM_SETTING_READ",
                                "SYSTEM_SETTING_MANAGE",
                                "AUDIT_READ")
                        .anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new org.springframework.security.core.userdetails.UsernameNotFoundException(username);
        };
    }
}
