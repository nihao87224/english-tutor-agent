package cn.forever24.tutor.infrastructure;

import cn.forever24.tutor.application.assessment.AssessmentApplicationService;
import cn.forever24.tutor.application.assessment.AssessmentAnswerRepository;
import cn.forever24.tutor.application.assessment.AssessmentResultRepository;
import cn.forever24.tutor.application.assessment.AssessmentSessionRepository;
import cn.forever24.tutor.application.assessment.OpenAnswerEvaluator;
import cn.forever24.tutor.application.conversation.ConversationApplicationService;
import cn.forever24.tutor.application.conversation.ConversationReplyStreamer;
import cn.forever24.tutor.application.assessment.SelfAssessmentRepository;
import cn.forever24.tutor.application.onboarding.OnboardingApplicationService;
import cn.forever24.tutor.application.onboarding.UserProfileRepository;
import cn.forever24.tutor.application.planning.LearningPlanApplicationService;
import cn.forever24.tutor.application.planning.LearningPlanRepository;
import cn.forever24.tutor.application.training.TrainingSessionApplicationService;
import cn.forever24.tutor.application.training.TrainingSessionRepository;
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
import cn.forever24.tutor.infrastructure.training.InMemoryTrainingSessionRepository;
import cn.forever24.tutor.infrastructure.training.JdbcTrainingSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

@Configuration
public class InfrastructureConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
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
            ConversationReplyStreamer conversationReplyStreamer
    ) {
        return new ConversationApplicationService(
                trainingSessionRepository,
                learningPlanRepository,
                conversationReplyStreamer);
    }

    @Bean
    public AssessmentApplicationService assessmentApplicationService(
            UserProfileRepository userProfileRepository,
            SelfAssessmentRepository selfAssessmentRepository,
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentAnswerRepository assessmentAnswerRepository,
            AssessmentResultRepository assessmentResultRepository,
            OpenAnswerEvaluator openAnswerEvaluator
    ) {
        return new AssessmentApplicationService(
                userProfileRepository,
                selfAssessmentRepository,
                assessmentSessionRepository,
                assessmentAnswerRepository,
                assessmentResultRepository,
                openAnswerEvaluator);
    }
}
