package cn.forever24.tutor.api.planning;

import cn.forever24.tutor.application.planning.LearningPlanApplicationService;
import cn.forever24.tutor.application.planning.LearningPlanRepository;
import cn.forever24.tutor.application.onboarding.UserProfileRepository;
import cn.forever24.tutor.planning.LearnerSkillState;
import cn.forever24.tutor.planning.LearningPlan;
import cn.forever24.tutor.planning.LearningPlanContext;
import cn.forever24.tutor.planning.RuleBasedTodayPlanGenerator;
import cn.forever24.tutor.profile.LearningPreferences;
import cn.forever24.tutor.profile.OnboardingProgress;
import cn.forever24.tutor.profile.OnboardingStep;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.PrivacySettings;
import cn.forever24.tutor.profile.ProfileSummary;
import cn.forever24.tutor.profile.UserKey;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LearningPlanControllerTest {

    private final LearningPlanController controller = new LearningPlanController(
            new LearningPlanApplicationService(
                    new ReadyProfileRepository(),
                    new FakeLearningPlanRepository(),
                    Clock.fixed(Instant.parse("2026-08-06T08:00:00Z"), ZoneOffset.UTC)));

    @Test
    void returnsTodayPlan() {
        LearningPlanResponse response = controller.getTodayPlan("user-1");

        assertEquals("plan-1", response.planId());
        assertEquals(LocalDate.parse("2026-08-06"), response.date());
        assertEquals(1, response.tasks().size());
        assertEquals("SPEAKING", response.tasks().get(0).type());
        assertEquals(false, response.temporaryAdjustment());
    }

    @Test
    void invalidPlanRequestMapsToBadRequestProblem() {
        ResponseEntity<?> response = controller.handleBadRequest(new IllegalArgumentException("not ready"));

        assertEquals(400, response.getStatusCode().value());
    }

    private static final class FakeLearningPlanRepository implements LearningPlanRepository {

        @Override
        public LearningPlan getOrGenerateTodayPlan(UserKey userKey, LocalDate planDate) {
            return RuleBasedTodayPlanGenerator.generate(new LearningPlanContext(
                    "plan-1",
                    planDate,
                    PrimaryGoal.GENERAL,
                    5,
                    1,
                    List.of(new LearnerSkillState(
                            "speaking",
                            new BigDecimal("0.4200"),
                            new BigDecimal("0.6000"),
                            "A2",
                            1))));
        }

        @Override
        public LearningPlan getPlan(UserKey userKey, String planId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void recordTrainingCompletion(UserKey userKey, String planId, List<String> practicedSkills, int evidenceCount) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class ReadyProfileRepository implements UserProfileRepository {

        @Override
        public ProfileSummary savePrimaryGoal(UserKey userKey, PrimaryGoal primaryGoal) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProfileSummary savePreferences(UserKey userKey, LearningPreferences preferences) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PrivacySettings getPrivacySettings(UserKey userKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PrivacySettings savePrivacySettings(UserKey userKey, PrivacySettings privacySettings) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void advanceOnboardingToAssessment(UserKey userKey) {
        }

        @Override
        public void advanceOnboardingToResult(UserKey userKey) {
        }

        @Override
        public OnboardingProgress getOnboardingProgress(UserKey userKey) {
            return new OnboardingProgress(OnboardingStep.RESULT, false, null);
        }
    }
}
