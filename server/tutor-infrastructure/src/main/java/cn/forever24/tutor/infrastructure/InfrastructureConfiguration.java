package cn.forever24.tutor.infrastructure;

import cn.forever24.tutor.application.assessment.AssessmentApplicationService;
import cn.forever24.tutor.application.assessment.AssessmentAnswerRepository;
import cn.forever24.tutor.application.assessment.AssessmentResultRepository;
import cn.forever24.tutor.application.assessment.AssessmentSessionRepository;
import cn.forever24.tutor.application.assessment.OpenAnswerEvaluator;
import cn.forever24.tutor.application.admin.AdminApplicationService;
import cn.forever24.tutor.application.admin.AdminRepository;
import cn.forever24.tutor.application.conversation.ConversationApplicationService;
import cn.forever24.tutor.application.conversation.ConversationReplyStreamer;
import cn.forever24.tutor.application.assessment.SelfAssessmentRepository;
import cn.forever24.tutor.application.auth.AccessTokenIssuer;
import cn.forever24.tutor.application.auth.AuthApplicationService;
import cn.forever24.tutor.application.auth.PasswordHasher;
import cn.forever24.tutor.application.auth.RefreshSessionRepository;
import cn.forever24.tutor.application.auth.RefreshTokenService;
import cn.forever24.tutor.application.auth.UserAccountRepository;
import cn.forever24.tutor.application.onboarding.OnboardingApplicationService;
import cn.forever24.tutor.application.onboarding.UserProfileRepository;
import cn.forever24.tutor.application.planning.LearningPlanApplicationService;
import cn.forever24.tutor.application.planning.LearningPlanRepository;
import cn.forever24.tutor.application.provider.AiProviderConfigurationApplicationService;
import cn.forever24.tutor.application.provider.AiProviderConfigurationRepository;
import cn.forever24.tutor.application.quota.DailyQuotaApplicationService;
import cn.forever24.tutor.application.quota.DailyQuotaRepository;
import cn.forever24.tutor.application.training.TrainingSessionApplicationService;
import cn.forever24.tutor.application.training.TrainingSessionRepository;
import cn.forever24.tutor.infrastructure.auth.BcryptPasswordHasher;
import cn.forever24.tutor.infrastructure.auth.HmacJwtAccessTokenService;
import cn.forever24.tutor.infrastructure.auth.InMemoryRefreshSessionRepository;
import cn.forever24.tutor.infrastructure.auth.InMemoryUserAccountRepository;
import cn.forever24.tutor.infrastructure.auth.JdbcRefreshSessionRepository;
import cn.forever24.tutor.infrastructure.auth.JdbcUserAccountRepository;
import cn.forever24.tutor.infrastructure.auth.Sha256RefreshTokenService;
import cn.forever24.tutor.infrastructure.admin.InMemoryAdminRepository;
import cn.forever24.tutor.infrastructure.admin.JdbcAdminRepository;
import cn.forever24.tutor.infrastructure.assessment.InMemoryAssessmentAnswerRepository;
import cn.forever24.tutor.infrastructure.assessment.InMemoryAssessmentSessionRepository;
import cn.forever24.tutor.infrastructure.assessment.InMemorySelfAssessmentRepository;
import cn.forever24.tutor.infrastructure.assessment.JdbcAssessmentAnswerRepository;
import cn.forever24.tutor.infrastructure.assessment.JdbcAssessmentResultRepository;
import cn.forever24.tutor.infrastructure.assessment.JdbcAssessmentSessionRepository;
import cn.forever24.tutor.infrastructure.assessment.JdbcSelfAssessmentRepository;
import cn.forever24.tutor.infrastructure.profile.InMemoryUserProfileRepository;
import cn.forever24.tutor.infrastructure.profile.JdbcUserProfileRepository;
import cn.forever24.tutor.infrastructure.planning.InMemoryLearningPlanRepository;
import cn.forever24.tutor.infrastructure.planning.JdbcLearningPlanRepository;
import cn.forever24.tutor.infrastructure.provider.AesGcmSecretCipher;
import cn.forever24.tutor.infrastructure.provider.AiProviderEnvironmentDefaults;
import cn.forever24.tutor.infrastructure.provider.InMemoryAiProviderConfigurationRepository;
import cn.forever24.tutor.infrastructure.provider.JdbcAiProviderConfigurationRepository;
import cn.forever24.tutor.infrastructure.quota.InMemoryDailyQuotaRepository;
import cn.forever24.tutor.infrastructure.quota.JdbcDailyQuotaRepository;
import cn.forever24.tutor.infrastructure.training.InMemoryTrainingSessionRepository;
import cn.forever24.tutor.infrastructure.training.JdbcTrainingSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

@Configuration
public class InfrastructureConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordHasher passwordHasher(PasswordEncoder passwordEncoder) {
        return new BcryptPasswordHasher(passwordEncoder);
    }

    @Bean
    @ConditionalOnMissingBean
    public RefreshTokenService refreshTokenService() {
        return new Sha256RefreshTokenService();
    }

    @Bean
    @ConditionalOnMissingBean
    public HmacJwtAccessTokenService accessTokenIssuer(
            Environment environment,
            Clock clock,
            ObjectProvider<ObjectMapper> objectMapperProvider
    ) {
        String secret = environment.getProperty("tutor.auth.jwt-signing-secret", "test-only-jwt-signing-secret-change-me-32");
        Duration ttl = environment.getProperty("tutor.auth.access-token-ttl", Duration.class, Duration.ofMinutes(15));
        return new HmacJwtAccessTokenService(secret, ttl, clock, objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean(UserAccountRepository.class)
    public UserAccountRepository userAccountRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            return new InMemoryUserAccountRepository();
        }
        return new JdbcUserAccountRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(RefreshSessionRepository.class)
    public RefreshSessionRepository refreshSessionRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            return new InMemoryRefreshSessionRepository();
        }
        return new JdbcRefreshSessionRepository(jdbcTemplate);
    }

    @Bean
    public AuthApplicationService authApplicationService(
            UserAccountRepository userAccountRepository,
            RefreshSessionRepository refreshSessionRepository,
            PasswordHasher passwordHasher,
            RefreshTokenService refreshTokenService,
            AccessTokenIssuer accessTokenIssuer,
            Clock clock
    ) {
        return new AuthApplicationService(
                userAccountRepository,
                refreshSessionRepository,
                passwordHasher,
                refreshTokenService,
                accessTokenIssuer,
                clock);
    }

    @Bean
    @ConditionalOnMissingBean(UserProfileRepository.class)
    public UserProfileRepository userProfileRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider, Clock clock) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            return new InMemoryUserProfileRepository();
        }
        return new JdbcUserProfileRepository(jdbcTemplate, clock);
    }

    @Bean
    @ConditionalOnMissingBean(SelfAssessmentRepository.class)
    public SelfAssessmentRepository selfAssessmentRepository(
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            Clock clock
    ) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            return new InMemorySelfAssessmentRepository();
        }
        return new JdbcSelfAssessmentRepository(jdbcTemplate, clock);
    }

    @Bean
    @ConditionalOnMissingBean(AssessmentSessionRepository.class)
    public AssessmentSessionRepository assessmentSessionRepository(
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            Clock clock
    ) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            return new InMemoryAssessmentSessionRepository();
        }
        return new JdbcAssessmentSessionRepository(jdbcTemplate, clock);
    }

    @Bean
    @ConditionalOnMissingBean(AssessmentAnswerRepository.class)
    public AssessmentAnswerRepository assessmentAnswerRepository(
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            Clock clock
    ) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            return new InMemoryAssessmentAnswerRepository();
        }
        return new JdbcAssessmentAnswerRepository(jdbcTemplate, clock);
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(AssessmentResultRepository.class)
    public AssessmentResultRepository assessmentResultRepository(
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            Clock clock,
            AssessmentAnswerRepository assessmentAnswerRepository
    ) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            if (assessmentAnswerRepository instanceof InMemoryAssessmentAnswerRepository inMemoryRepository) {
                return inMemoryRepository;
            }
            throw new IllegalStateException("in-memory assessment answer repository is required without JDBC");
        }
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new JdbcAssessmentResultRepository(jdbcTemplate, clock, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(LearningPlanRepository.class)
    public LearningPlanRepository learningPlanRepository(
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            Clock clock
    ) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            return new InMemoryLearningPlanRepository();
        }
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new JdbcLearningPlanRepository(jdbcTemplate, clock, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(TrainingSessionRepository.class)
    public TrainingSessionRepository trainingSessionRepository(
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            Clock clock
    ) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            return new InMemoryTrainingSessionRepository(clock);
        }
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new JdbcTrainingSessionRepository(jdbcTemplate, clock, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(DailyQuotaRepository.class)
    public DailyQuotaRepository dailyQuotaRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            return new InMemoryDailyQuotaRepository();
        }
        return new JdbcDailyQuotaRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public AesGcmSecretCipher aiProviderSecretCipher(Environment environment) {
        return new AesGcmSecretCipher(
                environment.getProperty("tutor.secret.encryption-key"),
                environment.getProperty("tutor.secret.encryption-key-version", "v1"));
    }

    @Bean
    @ConditionalOnMissingBean
    public AiProviderEnvironmentDefaults aiProviderEnvironmentDefaults(Environment environment) {
        return AiProviderEnvironmentDefaults.openAi(environment);
    }

    @Bean
    @ConditionalOnMissingBean(AiProviderConfigurationRepository.class)
    public AiProviderConfigurationRepository aiProviderConfigurationRepository(
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            AesGcmSecretCipher secretCipher,
            AiProviderEnvironmentDefaults defaults
    ) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            return new InMemoryAiProviderConfigurationRepository(secretCipher, defaults);
        }
        return new JdbcAiProviderConfigurationRepository(jdbcTemplate, secretCipher, defaults);
    }

    @Bean
    @ConditionalOnMissingBean
    public AiProviderConfigurationApplicationService aiProviderConfigurationApplicationService(
            AiProviderConfigurationRepository repository,
            Clock clock
    ) {
        return new AiProviderConfigurationApplicationService(repository, clock);
    }

    @Bean
    @ConditionalOnMissingBean(AdminRepository.class)
    public AdminRepository adminRepository(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            return new InMemoryAdminRepository();
        }
        return new JdbcAdminRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public AdminApplicationService adminApplicationService(AdminRepository adminRepository, Environment environment, Clock clock) {
        ZoneId resetZone = ZoneId.of(environment.getProperty("tutor.quota.reset-timezone", "Asia/Shanghai"));
        int defaultDailyLimit = environment.getProperty("tutor.quota.default-daily-limit", Integer.class, 50);
        return new AdminApplicationService(adminRepository, clock, resetZone, defaultDailyLimit);
    }

    @Bean
    public DailyQuotaApplicationService dailyQuotaApplicationService(
            DailyQuotaRepository dailyQuotaRepository,
            Environment environment,
            Clock clock
    ) {
        int defaultDailyLimit = environment.getProperty("tutor.quota.default-daily-limit", Integer.class, 50);
        ZoneId resetZone = ZoneId.of(environment.getProperty("tutor.quota.reset-timezone", "Asia/Shanghai"));
        return new DailyQuotaApplicationService(dailyQuotaRepository, clock, defaultDailyLimit, resetZone);
    }

    @Bean
    public OnboardingApplicationService onboardingApplicationService(UserProfileRepository userProfileRepository) {
        return new OnboardingApplicationService(userProfileRepository);
    }

    @Bean
    public LearningPlanApplicationService learningPlanApplicationService(
            UserProfileRepository userProfileRepository,
            LearningPlanRepository learningPlanRepository,
            Clock clock
    ) {
        return new LearningPlanApplicationService(userProfileRepository, learningPlanRepository, clock);
    }

    @Bean
    public TrainingSessionApplicationService trainingSessionApplicationService(
            UserProfileRepository userProfileRepository,
            LearningPlanRepository learningPlanRepository,
            TrainingSessionRepository trainingSessionRepository,
            Clock clock
    ) {
        return new TrainingSessionApplicationService(
                userProfileRepository,
                learningPlanRepository,
                trainingSessionRepository,
                clock);
    }

    @Bean
    public ConversationApplicationService conversationApplicationService(
            TrainingSessionRepository trainingSessionRepository,
            LearningPlanRepository learningPlanRepository,
            ConversationReplyStreamer conversationReplyStreamer,
            DailyQuotaApplicationService dailyQuotaApplicationService
    ) {
        return new ConversationApplicationService(
                trainingSessionRepository,
                learningPlanRepository,
                conversationReplyStreamer,
                dailyQuotaApplicationService);
    }

    @Bean
    public AssessmentApplicationService assessmentApplicationService(
            UserProfileRepository userProfileRepository,
            SelfAssessmentRepository selfAssessmentRepository,
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentAnswerRepository assessmentAnswerRepository,
            AssessmentResultRepository assessmentResultRepository,
            OpenAnswerEvaluator openAnswerEvaluator,
            DailyQuotaApplicationService dailyQuotaApplicationService
    ) {
        return new AssessmentApplicationService(
                userProfileRepository,
                selfAssessmentRepository,
                assessmentSessionRepository,
                assessmentAnswerRepository,
                assessmentResultRepository,
                openAnswerEvaluator,
                dailyQuotaApplicationService);
    }
}
