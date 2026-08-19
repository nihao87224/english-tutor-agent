package cn.forever24.tutor.application.assessment;

import cn.forever24.tutor.assessment.AssessmentAnswerReceipt;
import cn.forever24.tutor.assessment.AssessmentAnswerType;
import cn.forever24.tutor.application.onboarding.UserProfileRepository;
import cn.forever24.tutor.application.quota.DailyQuotaApplicationService;
import cn.forever24.tutor.application.quota.QuotaRequestType;
import cn.forever24.tutor.application.quota.QuotaReservation;
import cn.forever24.tutor.application.provider.AiProviderConfigurationException;
import cn.forever24.tutor.assessment.AssessmentResult;
import cn.forever24.tutor.assessment.AssessmentSession;
import cn.forever24.tutor.assessment.FourSkillSelfAssessment;
import cn.forever24.tutor.assessment.AssessmentItem;
import cn.forever24.tutor.assessment.InitialAssessmentItemBank;
import cn.forever24.tutor.assessment.ObjectiveAssessmentItem;
import cn.forever24.tutor.assessment.ObjectiveAssessmentItemBank;
import cn.forever24.tutor.assessment.OpenAnswerEvaluation;
import cn.forever24.tutor.assessment.OpenAssessmentItem;
import cn.forever24.tutor.assessment.OpenAssessmentItemBank;
import cn.forever24.tutor.assessment.SelfAssessmentResult;
import cn.forever24.tutor.assessment.ScoredObjectiveAnswer;
import cn.forever24.tutor.assessment.ScoredOpenAnswer;
import cn.forever24.tutor.profile.OnboardingProgress;
import cn.forever24.tutor.profile.OnboardingStep;
import cn.forever24.tutor.profile.UserKey;

import java.util.Optional;

public class AssessmentApplicationService {

    private final UserProfileRepository userProfileRepository;
    private final SelfAssessmentRepository selfAssessmentRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentAnswerRepository assessmentAnswerRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final OpenAnswerEvaluator openAnswerEvaluator;
    private final DailyQuotaApplicationService dailyQuotaApplicationService;

    public AssessmentApplicationService(
            UserProfileRepository userProfileRepository,
            SelfAssessmentRepository selfAssessmentRepository,
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentAnswerRepository assessmentAnswerRepository,
            AssessmentResultRepository assessmentResultRepository,
            OpenAnswerEvaluator openAnswerEvaluator,
            DailyQuotaApplicationService dailyQuotaApplicationService
    ) {
        this.userProfileRepository = userProfileRepository;
        this.selfAssessmentRepository = selfAssessmentRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentAnswerRepository = assessmentAnswerRepository;
        this.assessmentResultRepository = assessmentResultRepository;
        this.openAnswerEvaluator = openAnswerEvaluator;
        this.dailyQuotaApplicationService = dailyQuotaApplicationService;
    }

    public SelfAssessmentResult submitSelfAssessment(
            String userKeyValue,
            String listening,
            String speaking,
            String reading,
            String writing
    ) {
        UserKey userKey = new UserKey(userKeyValue);
        FourSkillSelfAssessment assessment = FourSkillSelfAssessment.fromContractValues(
                listening,
                speaking,
                reading,
                writing);
        OnboardingProgress progress = userProfileRepository.getOnboardingProgress(userKey);
        if (progress.step().ordinal() < OnboardingStep.SELF_ASSESSMENT.ordinal()) {
            throw new IllegalArgumentException("preferences must be saved before self assessment");
        }
        SelfAssessmentResult result = selfAssessmentRepository.save(userKey, assessment);
        userProfileRepository.advanceOnboardingToAssessment(userKey);
        return result;
    }

    public AssessmentSession startInitialAssessment(String userKeyValue, Integer targetMinutesValue) {
        UserKey userKey = new UserKey(userKeyValue);
        int targetMinutes = AssessmentSession.resolveTargetMinutes(targetMinutesValue);
        OnboardingProgress progress = userProfileRepository.getOnboardingProgress(userKey);
        if (progress.step().ordinal() < OnboardingStep.ASSESSMENT.ordinal()) {
            throw new IllegalArgumentException("self assessment must be submitted before starting assessment");
        }
        return assessmentSessionRepository.startOrResumeInitialAssessment(userKey, targetMinutes);
    }

    public Optional<AssessmentSession> getCurrentInitialAssessment(String userKeyValue) {
        UserKey userKey = new UserKey(userKeyValue);
        requireAssessmentStep(userKey);
        return assessmentSessionRepository.findActiveInitialAssessment(userKey);
    }

    public AssessmentItem getNextAssessmentItem(String userKeyValue, String assessmentId) {
        UserKey userKey = new UserKey(userKeyValue);
        requireAssessmentStep(userKey);
        return InitialAssessmentItemBank.nextUnanswered(assessmentAnswerRepository.answeredItemIds(userKey, assessmentId));
    }

    public AssessmentAnswerReceipt submitAssessmentAnswer(
            String userKeyValue,
            String assessmentId,
            String itemId,
            String answerTypeValue,
            String option,
            String text,
            Integer clientDurationMs
    ) {
        UserKey userKey = new UserKey(userKeyValue);
        AssessmentAnswerType answerType = AssessmentAnswerType.fromContractValue(answerTypeValue);
        if (answerType == AssessmentAnswerType.OPTION) {
            return submitObjectiveAnswer(userKey, assessmentId, itemId, option, clientDurationMs);
        }
        if (answerType == AssessmentAnswerType.TEXT) {
            return submitOpenTextAnswer(userKey, assessmentId, itemId, text, clientDurationMs);
        }
        throw new IllegalArgumentException("AUDIO answers require ASR and are not supported yet");
    }

    public AssessmentCompletion completeAssessment(String userKeyValue, String assessmentId) {
        UserKey userKey = new UserKey(userKeyValue);
        OnboardingProgress progress = requireAssessmentStep(userKey);
        if (progress.step().ordinal() >= OnboardingStep.RESULT.ordinal()
                && assessmentResultRepository.hasCompletedInitialAssessmentResult(userKey)) {
            AssessmentResult existingResult = assessmentResultRepository.getAssessmentResult(userKey, assessmentId);
            return new AssessmentCompletion(existingResult.assessmentId(), "COMPLETED");
        }
        if (!InitialAssessmentItemBank.allAnswered(assessmentAnswerRepository.answeredItemIds(userKey, assessmentId))) {
            throw new IllegalArgumentException("all initial assessment items must be answered before completion");
        }
        AssessmentResult result = assessmentResultRepository.completeInitialAssessment(userKey, assessmentId);
        userProfileRepository.advanceOnboardingToResult(userKey);
        return new AssessmentCompletion(result.assessmentId(), "COMPLETED");
    }

    public AssessmentResult getAssessmentResult(String userKeyValue, String assessmentId) {
        UserKey userKey = new UserKey(userKeyValue);
        return assessmentResultRepository.getAssessmentResult(userKey, assessmentId);
    }

    private AssessmentAnswerReceipt submitObjectiveAnswer(
            UserKey userKey,
            String assessmentId,
            String itemId,
            String option,
            Integer clientDurationMs
    ) {
        ObjectiveAssessmentItem item = ObjectiveAssessmentItemBank.requireObjectiveItem(itemId);
        ScoredObjectiveAnswer scoredAnswer = new ScoredObjectiveAnswer(
                item.itemId(),
                item.questionType(),
                option == null ? null : option.trim(),
                item.score(option),
                clientDurationMs);
        return assessmentAnswerRepository.saveObjectiveAnswer(userKey, assessmentId, scoredAnswer);
    }

    private OnboardingProgress requireAssessmentStep(UserKey userKey) {
        OnboardingProgress progress = userProfileRepository.getOnboardingProgress(userKey);
        if (progress.step().ordinal() < OnboardingStep.ASSESSMENT.ordinal()) {
            throw new IllegalArgumentException("self assessment must be submitted before starting assessment");
        }
        return progress;
    }

    private AssessmentAnswerReceipt submitOpenTextAnswer(
            UserKey userKey,
            String assessmentId,
            String itemId,
            String text,
            Integer clientDurationMs
    ) {
        OpenAssessmentItem item = OpenAssessmentItemBank.requireOpenTextItem(itemId);
        String normalizedText = OpenAssessmentItemBank.requireAnswerText(text);
        QuotaReservation reservation = dailyQuotaApplicationService.reserve(
                userKey.value(),
                QuotaRequestType.ASSESSMENT_OPEN_ANSWER,
                assessmentId + ":" + item.itemId());
        OpenAnswerEvaluation evaluation = evaluateOpenAnswer(item, normalizedText, reservation);
        ScoredOpenAnswer answer = new ScoredOpenAnswer(
                item.itemId(),
                item.questionType(),
                normalizedText,
                evaluation,
                clientDurationMs);
        return assessmentAnswerRepository.saveOpenAnswer(userKey, assessmentId, answer);
    }

    private OpenAnswerEvaluation evaluateOpenAnswer(OpenAssessmentItem item, String text, QuotaReservation reservation) {
        try {
            OpenAnswerEvaluation evaluation = openAnswerEvaluator.evaluate(new OpenAnswerEvaluationRequest(item, text));
            dailyQuotaApplicationService.commit(reservation);
            return evaluation;
        } catch (IllegalArgumentException exception) {
            dailyQuotaApplicationService.commit(reservation);
            return OpenAnswerEvaluation.safeUnscored(
                    openAnswerEvaluator.promptVersion(),
                    openAnswerEvaluator.schemaVersion());
        } catch (AiProviderConfigurationException exception) {
            dailyQuotaApplicationService.commit(reservation);
            return OpenAnswerEvaluation.safeUnscored(
                    openAnswerEvaluator.promptVersion(),
                    openAnswerEvaluator.schemaVersion());
        } catch (RuntimeException exception) {
            dailyQuotaApplicationService.refund(reservation);
            throw exception;
        }
    }
}
