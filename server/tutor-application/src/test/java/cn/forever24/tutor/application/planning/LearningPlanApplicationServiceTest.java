package cn.forever24.tutor.application.planning;

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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LearningPlanApplicationServiceTest {

    private final FakeUserProfileRepository userProfileRepository = new FakeUserProfileRepository();
    private final FakeLearningPlanRepository learningPlanRepository = new FakeLearningPlanRepository();
    private final LearningPlanApplicationService service = new LearningPlanApplicationService(
            userProfileRepository,
            learningPlanRepository,
            Clock.fixed(Instant.parse("2026-08-06T08:00:00Z"), ZoneOffset.UTC));

    @Test
    void getsTodayPlanAfterAssessmentResult() {
        userProfileRepository.step = OnboardingStep.RESULT;

        LearningPlan plan = service.getTodayPlan("user-1");

        assertEquals("plan-1", plan.planId());
        assertEquals(LocalDate.parse("2026-08-06"), learningPlanRepository.requestedDate);
    }

    @Test
    void rejectsUsersBeforeResultStep() {
        userProfileRepository.step = OnboardingStep.ASSESSMENT;

        assertThrows(IllegalArgumentException.class, () -> service.getTodayPlan("user-1"));

        assertEquals(0, learningPlanRepository.callCount);
    }

    private static final class FakeLearningPlanRepository implements LearningPlanRepository {

        private int callCount;
        private LocalDate requestedDate;

        @Override
        public LearningPlan getOrGenerateTodayPlan(UserKey userKey, LocalDate planDate) {
            callCount++;
            requestedDate = planDate;
            return RuleBasedTodayPlanGenerator.generate(new LearningPlanContext(
                    "plan-1",
                    planDate,
                    PrimaryGoal.GENERAL,
                    20,
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

    private static final class FakeUserProfileRepository implements UserProfileRepository {

        private OnboardingStep step = OnboardingStep.GOAL;

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
            step = OnboardingStep.ASSESSMENT;
        }

        @Override
        public void advanceOnboardingToResult(UserKey userKey) {
            step = OnboardingStep.RESULT;
        }

        @Override
        public OnboardingProgress getOnboardingProgress(UserKey userKey) {
            return new OnboardingProgress(step, step == OnboardingStep.COMPLETE, null);
        }
    }
}
