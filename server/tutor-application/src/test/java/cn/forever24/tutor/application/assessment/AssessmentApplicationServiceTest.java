package cn.forever24.tutor.application.assessment;

import cn.forever24.tutor.application.onboarding.UserProfileRepository;
import cn.forever24.tutor.application.quota.DailyQuotaApplicationService;
import cn.forever24.tutor.application.quota.TestDailyQuotaRepository;
import cn.forever24.tutor.assessment.AssessmentAttemptEvidence;
import cn.forever24.tutor.assessment.AssessmentAnswerReceipt;
import cn.forever24.tutor.assessment.AssessmentCorrectness;
import cn.forever24.tutor.assessment.AssessmentResult;
import cn.forever24.tutor.assessment.AssessmentSession;
import cn.forever24.tutor.assessment.AssessmentSessionStatus;
import cn.forever24.tutor.assessment.FourSkillSelfAssessment;
import cn.forever24.tutor.assessment.InitialAssessmentProfileGenerator;
import cn.forever24.tutor.assessment.OpenAnswerEvaluation;
import cn.forever24.tutor.assessment.SelfAssessmentResult;
import cn.forever24.tutor.assessment.SelfRating;
import cn.forever24.tutor.assessment.ScoredObjectiveAnswer;
import cn.forever24.tutor.assessment.ScoredOpenAnswer;
import cn.forever24.tutor.profile.LearningPreferences;
import cn.forever24.tutor.profile.OnboardingProgress;
import cn.forever24.tutor.profile.OnboardingStep;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.PrivacySettings;
import cn.forever24.tutor.profile.ProfileSummary;
import cn.forever24.tutor.profile.UserKey;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssessmentApplicationServiceTest {

    private final FakeUserProfileRepository profileRepository = new FakeUserProfileRepository();
    private final FakeSelfAssessmentRepository selfAssessmentRepository = new FakeSelfAssessmentRepository();
    private final FakeAssessmentSessionRepository assessmentSessionRepository = new FakeAssessmentSessionRepository();
    private final FakeAssessmentAnswerRepository assessmentAnswerRepository = new FakeAssessmentAnswerRepository();
    private final FakeAssessmentResultRepository assessmentResultRepository = new FakeAssessmentResultRepository();
    private final StubOpenAnswerEvaluator openAnswerEvaluator = new StubOpenAnswerEvaluator();
    private final DailyQuotaApplicationService dailyQuotaApplicationService = new DailyQuotaApplicationService(
            new TestDailyQuotaRepository(),
            Clock.systemUTC(),
            50,
            ZoneId.of("Asia/Shanghai"));
    private final AssessmentApplicationService service = new AssessmentApplicationService(
            profileRepository,
            selfAssessmentRepository,
            assessmentSessionRepository,
            assessmentAnswerRepository,
            assessmentResultRepository,
            openAnswerEvaluator,
            dailyQuotaApplicationService);

    @Test
    void submitsSelfAssessmentAndAdvancesProgressToAssessment() {
        profileRepository.step = OnboardingStep.SELF_ASSESSMENT;

        SelfAssessmentResult result = service.submitSelfAssessment(
                "user-1",
                "INTERMEDIATE",
                "BASIC",
                "UPPER_INTERMEDIATE",
                "INTERMEDIATE");

        assertNotNull(result.selfAssessmentId());
        assertEquals(SelfRating.INTERMEDIATE, result.estimatedBand());
        assertEquals(OnboardingStep.ASSESSMENT, profileRepository.step);
        assertEquals(SelfRating.BASIC, selfAssessmentRepository.saved.speaking());
    }

    @Test
    void rejectsSelfAssessmentBeforePreferences() {
        profileRepository.step = OnboardingStep.PREFERENCES;

        assertThrows(IllegalArgumentException.class,
                () -> service.submitSelfAssessment("user-1", "BASIC", "BASIC", "BASIC", "BASIC"));
    }

    @Test
    void rejectsInvalidRatingBeforePersisting() {
        profileRepository.step = OnboardingStep.SELF_ASSESSMENT;

        assertThrows(IllegalArgumentException.class,
                () -> service.submitSelfAssessment("user-1", "BASIC", "EXPERT", "BASIC", "BASIC"));

        assertEquals(null, selfAssessmentRepository.saved);
        assertEquals(OnboardingStep.SELF_ASSESSMENT, profileRepository.step);
    }

    @Test
    void startsInitialAssessmentWithDefaultTargetAfterSelfAssessment() {
        profileRepository.step = OnboardingStep.ASSESSMENT;

        AssessmentSession session = service.startInitialAssessment("user-1", null);

        assertEquals("assessment-1", session.assessmentId());
        assertEquals(AssessmentSessionStatus.IN_PROGRESS, session.status());
        assertEquals(9, session.targetMinutes());
        assertEquals(9, session.estimatedRemainingMinutes());
    }

    @Test
    void startsInitialAssessmentWithRequestedTarget() {
        profileRepository.step = OnboardingStep.ASSESSMENT;

        AssessmentSession session = service.startInitialAssessment("user-1", 12);

        assertEquals(12, session.targetMinutes());
        assertEquals(12, session.estimatedRemainingMinutes());
    }

    @Test
    void repeatedStartReturnsExistingActiveInitialSession() {
        profileRepository.step = OnboardingStep.ASSESSMENT;

        AssessmentSession first = service.startInitialAssessment("user-1", 10);
        AssessmentSession second = service.startInitialAssessment("user-1", 15);

        assertEquals(first.assessmentId(), second.assessmentId());
        assertEquals(10, second.targetMinutes());
        assertEquals(1, assessmentSessionRepository.savedCount);
    }

    @Test
    void rejectsInitialAssessmentBeforeSelfAssessmentSubmitted() {
        profileRepository.step = OnboardingStep.SELF_ASSESSMENT;

        assertThrows(IllegalArgumentException.class, () -> service.startInitialAssessment("user-1", null));

        assertEquals(0, assessmentSessionRepository.savedCount);
    }

    @Test
    void rejectsAssessmentTargetOutsideBoundsBeforePersisting() {
        profileRepository.step = OnboardingStep.ASSESSMENT;

        assertThrows(IllegalArgumentException.class, () -> service.startInitialAssessment("user-1", 4));
        assertThrows(IllegalArgumentException.class, () -> service.startInitialAssessment("user-1", 16));

        assertEquals(0, assessmentSessionRepository.savedCount);
    }

    @Test
    void submitsCorrectObjectiveAnswer() {
        AssessmentAnswerReceipt receipt = service.submitAssessmentAnswer(
                "user-1",
                "assessment-1",
                "initial-reading-1",
                "OPTION",
                "B",
                null,
                1200);

        assertEquals("answer-1", receipt.answerId());
        assertEquals(true, receipt.accepted());
        assertEquals(AssessmentCorrectness.CORRECT, assessmentAnswerRepository.saved.score().correctness());
        assertEquals("1.0000", assessmentAnswerRepository.saved.score().score().toPlainString());
        assertEquals(1200, assessmentAnswerRepository.saved.clientDurationMs());
    }

    @Test
    void submitsIncorrectObjectiveAnswer() {
        service.submitAssessmentAnswer("user-1", "assessment-1", "initial-reading-1", "OPTION", "A", null, null);

        assertEquals(AssessmentCorrectness.INCORRECT, assessmentAnswerRepository.saved.score().correctness());
        assertEquals("0.0000", assessmentAnswerRepository.saved.score().score().toPlainString());
    }

    @Test
    void repeatedObjectiveAnswerReturnsExistingReceipt() {
        AssessmentAnswerReceipt first = service.submitAssessmentAnswer(
                "user-1",
                "assessment-1",
                "initial-reading-1",
                "OPTION",
                "B",
                null,
                null);
        AssessmentAnswerReceipt second = service.submitAssessmentAnswer(
                "user-1",
                "assessment-1",
                "initial-reading-1",
                "OPTION",
                "A",
                null,
                null);

        assertEquals(first.answerId(), second.answerId());
        assertEquals(1, assessmentAnswerRepository.savedCount);
    }

    @Test
    void rejectsObjectiveItemSubmittedAsOpenTextBeforePersisting() {
        assertThrows(IllegalArgumentException.class, () -> service.submitAssessmentAnswer(
                "user-1",
                "assessment-1",
                "initial-reading-1",
                "TEXT",
                "B",
                "hello",
                null));

        assertEquals(0, assessmentAnswerRepository.savedCount);
    }

    @Test
    void rejectsUnknownObjectiveItemBeforePersisting() {
        assertThrows(IllegalArgumentException.class, () -> service.submitAssessmentAnswer(
                "user-1",
                "assessment-1",
                "unknown-item",
                "OPTION",
                "B",
                null,
                null));

        assertEquals(0, assessmentAnswerRepository.savedCount);
    }

    @Test
    void rejectsBlankObjectiveOptionBeforePersisting() {
        assertThrows(IllegalArgumentException.class, () -> service.submitAssessmentAnswer(
                "user-1",
                "assessment-1",
                "initial-reading-1",
                "OPTION",
                " ",
                null,
                null));

        assertEquals(0, assessmentAnswerRepository.savedCount);
    }

    @Test
    void propagatesInactiveAssessmentSessionError() {
        assertThrows(IllegalArgumentException.class, () -> service.submitAssessmentAnswer(
                "user-1",
                "missing-assessment",
                "initial-reading-1",
                "OPTION",
                "B",
                null,
                null));

        assertEquals(0, assessmentAnswerRepository.savedCount);
    }

    @Test
    void submitsOpenTextAnswerWithEvaluatorResult() {
        AssessmentAnswerReceipt receipt = service.submitAssessmentAnswer(
                "user-1",
                "assessment-1",
                "initial-speaking-open-1",
                "TEXT",
                null,
                "I think it was delayed because the team needed more time.",
                1800);

        assertEquals("answer-1", receipt.answerId());
        assertEquals(AssessmentCorrectness.CORRECT, assessmentAnswerRepository.savedOpen.evaluation().correctness());
        assertEquals("0.7600", assessmentAnswerRepository.savedOpen.evaluation().score().toPlainString());
        assertEquals("open-answer-evaluator-v1", assessmentAnswerRepository.savedOpen.evaluation().promptVersion());
        assertEquals("I think it was delayed because the team needed more time.", assessmentAnswerRepository.savedOpen.text());
    }

    @Test
    void invalidOpenAnswerEvaluatorOutputFallsBackToUnscored() {
        openAnswerEvaluator.fail = true;

        service.submitAssessmentAnswer(
                "user-1",
                "assessment-1",
                "initial-speaking-open-1",
                "TEXT",
                null,
                "Short answer.",
                null);

        assertEquals(AssessmentCorrectness.UNSCORED, assessmentAnswerRepository.savedOpen.evaluation().correctness());
        assertEquals("0.0000", assessmentAnswerRepository.savedOpen.evaluation().score().toPlainString());
        assertEquals("0.0000", assessmentAnswerRepository.savedOpen.evaluation().evaluatorConfidence().toPlainString());
    }

    @Test
    void repeatedOpenTextAnswerReturnsExistingReceipt() {
        AssessmentAnswerReceipt first = service.submitAssessmentAnswer(
                "user-1",
                "assessment-1",
                "initial-speaking-open-1",
                "TEXT",
                null,
                "First answer because it has a reason.",
                null);
        AssessmentAnswerReceipt second = service.submitAssessmentAnswer(
                "user-1",
                "assessment-1",
                "initial-speaking-open-1",
                "TEXT",
                null,
                "Different answer.",
                null);

        assertEquals(first.answerId(), second.answerId());
        assertEquals(1, assessmentAnswerRepository.savedCount);
    }

    @Test
    void rejectsBlankOpenTextBeforePersisting() {
        assertThrows(IllegalArgumentException.class, () -> service.submitAssessmentAnswer(
                "user-1",
                "assessment-1",
                "initial-speaking-open-1",
                "TEXT",
                null,
                " ",
                null));

        assertEquals(0, assessmentAnswerRepository.savedCount);
    }

    @Test
    void rejectsAudioAnswersUntilAsrExists() {
        assertThrows(IllegalArgumentException.class, () -> service.submitAssessmentAnswer(
                "user-1",
                "assessment-1",
                "initial-speaking-open-1",
                "AUDIO",
                null,
                null,
                null));

        assertEquals(0, assessmentAnswerRepository.savedCount);
    }

    @Test
    void completesAssessmentAndAdvancesProgressToResult() {
        profileRepository.step = OnboardingStep.ASSESSMENT;

        AssessmentCompletion completion = service.completeAssessment("user-1", "assessment-1");

        assertEquals("assessment-1", completion.assessmentId());
        assertEquals("COMPLETED", completion.status());
        assertEquals(OnboardingStep.RESULT, profileRepository.step);
        assertEquals(1, assessmentResultRepository.completeCount);
    }

    @Test
    void resultStepWithoutPersistedResultCanCompleteAssessment() {
        profileRepository.step = OnboardingStep.RESULT;
        assessmentResultRepository.hasResult = false;

        AssessmentCompletion completion = service.completeAssessment("user-1", "assessment-1");

        assertEquals("assessment-1", completion.assessmentId());
        assertEquals("COMPLETED", completion.status());
        assertEquals(1, assessmentResultRepository.completeCount);
        assertEquals(true, assessmentResultRepository.hasResult);
    }

    @Test
    void rejectsAssessmentCompletionBeforeAssessmentStep() {
        profileRepository.step = OnboardingStep.SELF_ASSESSMENT;

        assertThrows(IllegalArgumentException.class, () -> service.completeAssessment("user-1", "assessment-1"));

        assertEquals(0, assessmentResultRepository.completeCount);
    }

    @Test
    void returnsPersistedAssessmentResult() {
        AssessmentResult result = service.getAssessmentResult("user-1", "assessment-1");

        assertEquals("assessment-1", result.assessmentId());
        assertEquals(8, result.skills().size());
        assertEquals(true, result.skills().containsKey("reading"));
    }

    private static final class FakeSelfAssessmentRepository implements SelfAssessmentRepository {

        private FourSkillSelfAssessment saved;

        @Override
        public SelfAssessmentResult save(UserKey userKey, FourSkillSelfAssessment assessment) {
            this.saved = assessment;
            return new SelfAssessmentResult("self-1", assessment.estimatedBand());
        }
    }

    private static final class FakeAssessmentSessionRepository implements AssessmentSessionRepository {

        private final Map<UserKey, AssessmentSession> activeSessions = new HashMap<>();
        private int savedCount;

        @Override
        public AssessmentSession startOrResumeInitialAssessment(UserKey userKey, int targetMinutes) {
            return activeSessions.computeIfAbsent(userKey, ignored -> {
                savedCount++;
                return new AssessmentSession(
                        "assessment-" + savedCount,
                        AssessmentSessionStatus.IN_PROGRESS,
                        targetMinutes,
                        targetMinutes);
            });
        }

        @Override
        public Optional<AssessmentSession> findActiveInitialAssessment(UserKey userKey) {
            return Optional.of(new AssessmentSession("assessment-1", AssessmentSessionStatus.IN_PROGRESS, 9, 9));
        }
    }

    private static final class FakeAssessmentAnswerRepository implements AssessmentAnswerRepository {

        private final Map<String, AssessmentAnswerReceipt> receipts = new HashMap<>();
        private ScoredObjectiveAnswer saved;
        private ScoredOpenAnswer savedOpen;
        private int savedCount;

        @Override
        public AssessmentAnswerReceipt saveObjectiveAnswer(
                UserKey userKey,
                String assessmentId,
                ScoredObjectiveAnswer answer
        ) {
            if ("missing-assessment".equals(assessmentId)) {
                throw new IllegalArgumentException("active assessment session was not found");
            }
            String key = userKey.value() + ":" + assessmentId + ":" + answer.itemId();
            return receipts.computeIfAbsent(key, ignored -> {
                saved = answer;
                savedCount++;
                return new AssessmentAnswerReceipt("answer-" + savedCount, true);
            });
        }

        @Override
        public AssessmentAnswerReceipt saveOpenAnswer(
                UserKey userKey,
                String assessmentId,
                ScoredOpenAnswer answer
        ) {
            if ("missing-assessment".equals(assessmentId)) {
                throw new IllegalArgumentException("active assessment session was not found");
            }
            String key = userKey.value() + ":" + assessmentId + ":" + answer.itemId();
            return receipts.computeIfAbsent(key, ignored -> {
                savedOpen = answer;
                savedCount++;
                return new AssessmentAnswerReceipt("answer-" + savedCount, true);
            });
        }

        @Override
        public Set<String> answeredItemIds(UserKey userKey, String assessmentId) {
            return Set.of("initial-reading-1", "initial-listening-1", "initial-grammar-1", "initial-speaking-open-1", "initial-writing-open-1");
        }
    }

    private static final class FakeAssessmentResultRepository implements AssessmentResultRepository {

        private int completeCount;
        private boolean hasResult;

        @Override
        public AssessmentResult completeInitialAssessment(UserKey userKey, String assessmentId) {
            if ("missing-assessment".equals(assessmentId)) {
                throw new IllegalArgumentException("assessment session was not found");
            }
            completeCount++;
            hasResult = true;
            return result(assessmentId);
        }

        @Override
        public AssessmentResult getAssessmentResult(UserKey userKey, String assessmentId) {
            if ("missing-assessment".equals(assessmentId)) {
                throw new IllegalArgumentException("assessment result was not found");
            }
            return result(assessmentId);
        }

        @Override
        public boolean hasCompletedInitialAssessmentResult(UserKey userKey) {
            return hasResult;
        }

        private AssessmentResult result(String assessmentId) {
            return InitialAssessmentProfileGenerator.generate(
                    assessmentId,
                    List.of(new AssessmentAttemptEvidence(
                            "initial-reading-1",
                            AssessmentCorrectness.CORRECT,
                            BigDecimal.ONE,
                            BigDecimal.ONE)));
        }
    }

    private static final class StubOpenAnswerEvaluator implements OpenAnswerEvaluator {

        private boolean fail;

        @Override
        public String promptVersion() {
            return "open-answer-evaluator-v1";
        }

        @Override
        public String schemaVersion() {
            return "open-answer-evaluation-v1";
        }

        @Override
        public OpenAnswerEvaluation evaluate(OpenAnswerEvaluationRequest request) {
            if (fail) {
                throw new IllegalArgumentException("invalid evaluator output");
            }
            return new OpenAnswerEvaluation(
                    AssessmentCorrectness.CORRECT,
                    new java.math.BigDecimal("0.7600"),
                    new java.math.BigDecimal("1.0000"),
                    "Clear response with a reason.",
                    promptVersion(),
                    schemaVersion());
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
