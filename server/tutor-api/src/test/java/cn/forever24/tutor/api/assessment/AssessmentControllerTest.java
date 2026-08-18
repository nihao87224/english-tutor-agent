package cn.forever24.tutor.api.assessment;

import cn.forever24.tutor.api.auth.CurrentUserKeyResolver;
import cn.forever24.tutor.application.assessment.AssessmentApplicationService;
import cn.forever24.tutor.application.assessment.AssessmentResultRepository;
import cn.forever24.tutor.application.assessment.AssessmentAnswerRepository;
import cn.forever24.tutor.application.assessment.AssessmentSessionRepository;
import cn.forever24.tutor.application.assessment.OpenAnswerEvaluationRequest;
import cn.forever24.tutor.application.assessment.OpenAnswerEvaluator;
import cn.forever24.tutor.application.assessment.SelfAssessmentRepository;
import cn.forever24.tutor.application.quota.DailyQuotaApplicationService;
import cn.forever24.tutor.application.quota.DailyQuotaRepository;
import cn.forever24.tutor.application.quota.DailyQuotaStatus;
import cn.forever24.tutor.application.quota.QuotaPolicy;
import cn.forever24.tutor.application.quota.QuotaRequestType;
import cn.forever24.tutor.application.quota.QuotaReservation;
import cn.forever24.tutor.application.quota.QuotaReservationStatus;
import cn.forever24.tutor.application.onboarding.UserProfileRepository;
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
import cn.forever24.tutor.assessment.ScoredObjectiveAnswer;
import cn.forever24.tutor.assessment.ScoredOpenAnswer;
import cn.forever24.tutor.profile.LearningPreferences;
import cn.forever24.tutor.profile.OnboardingProgress;
import cn.forever24.tutor.profile.OnboardingStep;
import cn.forever24.tutor.profile.PrimaryGoal;
import cn.forever24.tutor.profile.PrivacySettings;
import cn.forever24.tutor.profile.ProfileSummary;
import cn.forever24.tutor.profile.UserKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssessmentControllerTest {

    private final AssessmentController controller = new AssessmentController(
            new AssessmentApplicationService(
                    new ReadyProfileRepository(),
                    new FakeSelfAssessmentRepository(),
                    new FakeAssessmentSessionRepository(),
                    new FakeAssessmentAnswerRepository(),
                    new FakeAssessmentResultRepository(),
                    new StubOpenAnswerEvaluator(),
                    new DailyQuotaApplicationService(
                            new AllowingDailyQuotaRepository(),
                            Clock.systemUTC(),
                            50,
                            ZoneId.of("Asia/Shanghai"))),
            new CurrentUserKeyResolver(ignored -> "user-1"));

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("1", null, "ROLE_USER"));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitsSelfAssessment() {
        SelfAssessmentResponse response = controller.submitSelfAssessment(
                new SelfAssessmentRequest("INTERMEDIATE", "BASIC", "INTERMEDIATE", "UPPER_INTERMEDIATE"));

        assertEquals("self-1", response.selfAssessmentId());
        assertEquals("INTERMEDIATE", response.estimatedBand());
    }

    @Test
    void invalidAssessmentMapsToBadRequestProblem() {
        ResponseEntity<?> response = controller.handleBadRequest(new IllegalArgumentException("unsupported rating"));

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void startsAssessmentWithDefaultTarget() {
        AssessmentSessionResponse response = controller.startAssessment(null);

        assertEquals("assessment-1", response.assessmentId());
        assertEquals("IN_PROGRESS", response.status());
        assertEquals(9, response.targetMinutes());
        assertEquals(9, response.estimatedRemainingMinutes());
    }

    @Test
    void startsAssessmentWithRequestedTarget() {
        AssessmentSessionResponse response = controller.startAssessment(new StartAssessmentRequest(12));

        assertEquals(12, response.targetMinutes());
        assertEquals(12, response.estimatedRemainingMinutes());
    }

    @Test
    void submitsAssessmentAnswer() {
        AnswerReceiptResponse response = controller.submitAssessmentAnswer(
                "assessment-1",
                new AssessmentAnswerRequest("initial-reading-1", "OPTION", "B", null, null, 900));

        assertEquals("answer-1", response.answerId());
        assertEquals(true, response.accepted());
    }

    @Test
    void submitsOpenAssessmentAnswer() {
        AnswerReceiptResponse response = controller.submitAssessmentAnswer(
                "assessment-1",
                new AssessmentAnswerRequest(
                        "initial-speaking-open-1",
                        "TEXT",
                        null,
                        "It was delayed because the team needed more time.",
                        null,
                        1200));

        assertEquals("answer-1", response.answerId());
        assertEquals(true, response.accepted());
    }

    @Test
    void completesAssessment() {
        AssessmentCompletionResponse response = controller.completeAssessment("assessment-1");

        assertEquals("assessment-1", response.assessmentId());
        assertEquals("COMPLETED", response.status());
    }

    @Test
    void returnsAssessmentResult() {
        AssessmentResultResponse response = controller.getAssessmentResult("assessment-1");

        assertEquals("assessment-1", response.assessmentId());
        assertEquals(8, response.skills().size());
        assertEquals(true, response.skills().containsKey("reading"));
    }

    private static final class FakeSelfAssessmentRepository implements SelfAssessmentRepository {

        @Override
        public SelfAssessmentResult save(UserKey userKey, FourSkillSelfAssessment assessment) {
            return new SelfAssessmentResult("self-1", assessment.estimatedBand());
        }
    }

    private static final class FakeAssessmentSessionRepository implements AssessmentSessionRepository {

        @Override
        public AssessmentSession startOrResumeInitialAssessment(UserKey userKey, int targetMinutes) {
            return new AssessmentSession(
                    "assessment-1",
                    AssessmentSessionStatus.IN_PROGRESS,
                    targetMinutes,
                    targetMinutes);
        }
    }

    private static final class FakeAssessmentAnswerRepository implements AssessmentAnswerRepository {

        @Override
        public AssessmentAnswerReceipt saveObjectiveAnswer(
                UserKey userKey,
                String assessmentId,
                ScoredObjectiveAnswer answer
        ) {
            return new AssessmentAnswerReceipt("answer-1", true);
        }

        @Override
        public AssessmentAnswerReceipt saveOpenAnswer(
                UserKey userKey,
                String assessmentId,
                ScoredOpenAnswer answer
        ) {
            return new AssessmentAnswerReceipt("answer-1", true);
        }
    }

    private static final class FakeAssessmentResultRepository implements AssessmentResultRepository {

        @Override
        public AssessmentResult completeInitialAssessment(UserKey userKey, String assessmentId) {
            return result(assessmentId);
        }

        @Override
        public AssessmentResult getAssessmentResult(UserKey userKey, String assessmentId) {
            return result(assessmentId);
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
            return OpenAnswerEvaluation.safeUnscored(promptVersion(), schemaVersion());
        }
    }

    private static final class AllowingDailyQuotaRepository implements DailyQuotaRepository {

        @Override
        public DailyQuotaStatus getStatus(
                UserKey userKey,
                LocalDate quotaDate,
                QuotaPolicy policy,
                OffsetDateTime resetAt
        ) {
            return new DailyQuotaStatus(quotaDate, policy.dailyLimit(), 0, 0, policy.dailyLimit(), policy.unlimited(), resetAt);
        }

        @Override
        public QuotaReservation reserve(
                UserKey userKey,
                LocalDate quotaDate,
                QuotaRequestType requestType,
                String idempotencyKey,
                QuotaPolicy policy,
                Instant now,
                Instant expiresAt,
                OffsetDateTime resetAt
        ) {
            return new QuotaReservation(
                    "quota-test",
                    userKey.value(),
                    quotaDate,
                    requestType,
                    idempotencyKey,
                    QuotaReservationStatus.RESERVED);
        }

        @Override
        public void commit(String reservationId, Instant now) {
        }

        @Override
        public void refund(String reservationId, Instant now) {
        }

        @Override
        public void refundStaleReservations(Instant now) {
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
            return new OnboardingProgress(OnboardingStep.ASSESSMENT, false, null);
        }
    }
}
