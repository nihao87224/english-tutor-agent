package cn.forever24.tutor.android

import cn.forever24.tutor.android.ui.CorrectionStyle
import cn.forever24.tutor.android.ui.OnboardingStep
import cn.forever24.tutor.android.ui.PrimaryGoal
import cn.forever24.tutor.android.ui.SelfAssessmentSkill
import cn.forever24.tutor.android.ui.SelfRating
import cn.forever24.tutor.android.ui.SkillScoreUiModel
import cn.forever24.tutor.android.ui.TodayPlanTaskUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainViewModelTest {

    @Test
    fun initialStateRequiresOnePrimaryGoal() {
        val viewModel = MainViewModel()

        assertEquals("Choose your learning goal", viewModel.uiState.value.title)
        assertEquals(3, viewModel.uiState.value.availableGoals.size)
        assertFalse(viewModel.uiState.value.canContinue)
        assertFalse(viewModel.canSubmitGoal())
        assertEquals(20, viewModel.uiState.value.dailyMinutes)
        assertEquals(CorrectionStyle.STANDARD, viewModel.uiState.value.correctionStyle)
        assertEquals(OnboardingStep.GOAL, viewModel.uiState.value.currentOnboardingStep)
        assertFalse(viewModel.canSubmitSelfAssessment())
        assertFalse(viewModel.uiState.value.reminderEnabled)
        assertTrue(viewModel.uiState.value.saveRawText)
        assertTrue(viewModel.uiState.value.saveRawAudio)
    }

    @Test
    fun selectingGoalEnablesContinue() {
        val viewModel = MainViewModel()

        viewModel.selectGoal(PrimaryGoal.WORKPLACE)

        assertEquals(PrimaryGoal.WORKPLACE, viewModel.uiState.value.selectedGoal)
        assertTrue(viewModel.uiState.value.canContinue)
        assertTrue(viewModel.canSubmitGoal())
    }

    @Test
    fun selectingAnotherGoalKeepsOnlyOnePrimaryGoal() {
        val viewModel = MainViewModel()

        viewModel.selectGoal(PrimaryGoal.WORKPLACE)
        viewModel.selectGoal(PrimaryGoal.IELTS)

        assertEquals(PrimaryGoal.IELTS, viewModel.uiState.value.selectedGoal)
    }

    @Test
    fun preferenceStateCanBeUpdatedAfterGoalSelection() {
        val viewModel = MainViewModel()

        viewModel.selectGoal(PrimaryGoal.GENERAL)
        viewModel.selectDailyMinutes(30)
        viewModel.selectCorrectionStyle(CorrectionStyle.LIGHT)
        viewModel.setReminderEnabled(true)
        viewModel.setSaveRawText(false)
        viewModel.setSaveRawAudio(false)

        assertEquals(30, viewModel.uiState.value.dailyMinutes)
        assertEquals(CorrectionStyle.LIGHT, viewModel.uiState.value.correctionStyle)
        assertTrue(viewModel.uiState.value.reminderEnabled)
        assertFalse(viewModel.uiState.value.saveRawText)
        assertFalse(viewModel.uiState.value.saveRawAudio)
        assertTrue(viewModel.canSubmitPreferences())
    }

    @Test
    fun invalidDailyMinutesAreIgnored() {
        val viewModel = MainViewModel()

        viewModel.selectDailyMinutes(17)

        assertEquals(20, viewModel.uiState.value.dailyMinutes)
    }

    @Test
    fun appliesRecoveredSelfAssessmentProgress() {
        val viewModel = MainViewModel()

        viewModel.applyOnboardingProgress("SELF_ASSESSMENT", completed = false, assessmentId = null)

        assertEquals(OnboardingStep.SELF_ASSESSMENT, viewModel.uiState.value.currentOnboardingStep)
        assertFalse(viewModel.uiState.value.onboardingCompleted)
    }

    @Test
    fun preservesAssessmentIdOnlyForAssessmentStep() {
        val viewModel = MainViewModel()

        viewModel.applyOnboardingProgress("ASSESSMENT", completed = false, assessmentId = "assessment-1")
        assertEquals("assessment-1", viewModel.uiState.value.assessmentId)

        viewModel.applyOnboardingProgress("RESULT", completed = false, assessmentId = "assessment-1")
        assertEquals(null, viewModel.uiState.value.assessmentId)
    }

    @Test
    fun appliesAssessmentSessionState() {
        val viewModel = MainViewModel()

        viewModel.applyAssessmentSession(
            assessmentId = "assessment-1",
            status = "IN_PROGRESS",
            targetMinutes = 9,
            estimatedRemainingMinutes = 8,
        )

        assertEquals(OnboardingStep.ASSESSMENT, viewModel.uiState.value.currentOnboardingStep)
        assertEquals("assessment-1", viewModel.uiState.value.assessmentId)
        assertEquals("IN_PROGRESS", viewModel.uiState.value.assessmentStatus)
        assertEquals(9, viewModel.uiState.value.assessmentTargetMinutes)
        assertEquals(8, viewModel.uiState.value.assessmentEstimatedRemainingMinutes)
    }

    @Test
    fun ignoresInvalidAssessmentSessionState() {
        val viewModel = MainViewModel()

        viewModel.applyAssessmentSession(
            assessmentId = "",
            status = "IN_PROGRESS",
            targetMinutes = 4,
            estimatedRemainingMinutes = 4,
        )

        assertEquals(null, viewModel.uiState.value.assessmentId)
        assertEquals(null, viewModel.uiState.value.assessmentTargetMinutes)
    }

    @Test
    fun appliesAssessmentAnswerReceipt() {
        val viewModel = MainViewModel()

        viewModel.applyAssessmentAnswerReceipt(answerId = "answer-1", accepted = true)

        assertEquals("answer-1", viewModel.uiState.value.latestAssessmentAnswerId)
        assertTrue(viewModel.uiState.value.latestAssessmentAnswerAccepted)
    }

    @Test
    fun ignoresBlankAssessmentAnswerReceipt() {
        val viewModel = MainViewModel()

        viewModel.applyAssessmentAnswerReceipt(answerId = " ", accepted = true)

        assertEquals(null, viewModel.uiState.value.latestAssessmentAnswerId)
        assertFalse(viewModel.uiState.value.latestAssessmentAnswerAccepted)
    }

    @Test
    fun appliesOpenAnswerEvaluation() {
        val viewModel = MainViewModel()

        viewModel.applyOpenAnswerEvaluation(
            feedback = "Clear response with a reason.",
            scorePercent = 76,
        )

        assertEquals("Clear response with a reason.", viewModel.uiState.value.latestOpenAnswerFeedback)
        assertEquals(76, viewModel.uiState.value.latestOpenAnswerScorePercent)
    }

    @Test
    fun ignoresInvalidOpenAnswerEvaluation() {
        val viewModel = MainViewModel()

        viewModel.applyOpenAnswerEvaluation(feedback = " ", scorePercent = 101)

        assertEquals(null, viewModel.uiState.value.latestOpenAnswerFeedback)
        assertEquals(null, viewModel.uiState.value.latestOpenAnswerScorePercent)
    }

    @Test
    fun completedFlagOnlyAppliesToCompleteStep() {
        val viewModel = MainViewModel()

        viewModel.applyOnboardingProgress("RESULT", completed = true, assessmentId = null)
        assertFalse(viewModel.uiState.value.onboardingCompleted)

        viewModel.applyOnboardingProgress("COMPLETE", completed = true, assessmentId = null)
        assertTrue(viewModel.uiState.value.onboardingCompleted)
    }

    @Test
    fun unknownRecoveredStepFallsBackToGoal() {
        val viewModel = MainViewModel()

        viewModel.applyOnboardingProgress("BROKEN", completed = false, assessmentId = null)

        assertEquals(OnboardingStep.GOAL, viewModel.uiState.value.currentOnboardingStep)
    }

    @Test
    fun selfAssessmentRequiresAllFourSkills() {
        val viewModel = MainViewModel()

        viewModel.selectSelfRating(SelfAssessmentSkill.LISTENING, SelfRating.INTERMEDIATE)
        viewModel.selectSelfRating(SelfAssessmentSkill.SPEAKING, SelfRating.BASIC)
        viewModel.selectSelfRating(SelfAssessmentSkill.READING, SelfRating.UPPER_INTERMEDIATE)

        assertFalse(viewModel.canSubmitSelfAssessment())

        viewModel.selectSelfRating(SelfAssessmentSkill.WRITING, SelfRating.INTERMEDIATE)

        assertTrue(viewModel.canSubmitSelfAssessment())
    }

    @Test
    fun selectingSelfRatingReplacesPreviousValueForSkill() {
        val viewModel = MainViewModel()

        viewModel.selectSelfRating(SelfAssessmentSkill.SPEAKING, SelfRating.BASIC)
        viewModel.selectSelfRating(SelfAssessmentSkill.SPEAKING, SelfRating.ADVANCED)

        assertEquals(SelfRating.ADVANCED, viewModel.uiState.value.selfRatings[SelfAssessmentSkill.SPEAKING])
    }

    @Test
    fun firstUseFlowReachesResultAndTodayPlan() {
        val viewModel = MainViewModel()

        viewModel.selectGoal(PrimaryGoal.WORKPLACE)
        viewModel.selectDailyMinutes(20)
        viewModel.selectCorrectionStyle(CorrectionStyle.STANDARD)
        viewModel.setReminderEnabled(true)
        viewModel.selectSelfRating(SelfAssessmentSkill.LISTENING, SelfRating.INTERMEDIATE)
        viewModel.selectSelfRating(SelfAssessmentSkill.SPEAKING, SelfRating.BASIC)
        viewModel.selectSelfRating(SelfAssessmentSkill.READING, SelfRating.INTERMEDIATE)
        viewModel.selectSelfRating(SelfAssessmentSkill.WRITING, SelfRating.INTERMEDIATE)
        viewModel.applyOnboardingProgress("ASSESSMENT", completed = false, assessmentId = "assessment-1")
        viewModel.applyAssessmentSession(
            assessmentId = "assessment-1",
            status = "IN_PROGRESS",
            targetMinutes = 9,
            estimatedRemainingMinutes = 8,
        )
        viewModel.applyAssessmentAnswerReceipt(answerId = "answer-1", accepted = true)
        viewModel.applyOpenAnswerEvaluation(feedback = "Clear response with a reason.", scorePercent = 76)
        viewModel.applyAssessmentCompletion(assessmentId = "assessment-1", status = "COMPLETED")
        viewModel.applyAssessmentResult(
            assessmentId = "assessment-1",
            overallLevel = "A2",
            confidencePercent = 54,
            summary = "Initial profile; later evidence will keep calibrating it.",
            strengths = listOf("Reading is a relative strength."),
            priorities = listOf("Speaking should be trained first."),
            skills = listOf(
                SkillScoreUiModel(
                    skill = "speaking",
                    scorePercent = 42,
                    level = "A2",
                    confidencePercent = 60,
                ),
            ),
        )
        viewModel.applyTodayPlan(
            planId = "plan-1",
            date = "2026-08-06",
            totalMinutes = 20,
            reasons = listOf("Your main goal is workplace communication, so speaking is first today."),
            tasks = listOf(
                TodayPlanTaskUiModel(
                    taskId = "task-1",
                    type = "CONVERSATION",
                    title = "工作场景快速回应",
                    durationMinutes = 20,
                    skillFocus = listOf("speaking"),
                    difficulty = "EASY",
                    reason = "The initial profile estimates speaking at A2.",
                ),
            ),
        )

        val state = viewModel.uiState.value
        assertEquals(PrimaryGoal.WORKPLACE, state.selectedGoal)
        assertTrue(viewModel.canSubmitPreferences())
        assertTrue(viewModel.canSubmitSelfAssessment())
        assertEquals(OnboardingStep.RESULT, state.currentOnboardingStep)
        assertFalse(state.onboardingCompleted)
        assertEquals("assessment-1", state.assessmentResult?.assessmentId)
        assertEquals("A2", state.assessmentResult?.overallLevel)
        assertEquals("plan-1", state.todayPlan?.planId)
        assertEquals(1, state.todayPlan?.tasks?.size)
        assertTrue(state.canStartFirstTraining)
    }

    @Test
    fun ignoresInvalidTodayPlan() {
        val viewModel = MainViewModel()

        viewModel.applyTodayPlan(
            planId = "plan-1",
            date = "2026-08-06",
            totalMinutes = 20,
            reasons = listOf("reason"),
            tasks = listOf(
                TodayPlanTaskUiModel(
                    taskId = "task-1",
                    type = "SPEAKING",
                    title = "Short answer",
                    durationMinutes = 5,
                    skillFocus = listOf("speaking"),
                    difficulty = "EASY",
                    reason = "reason",
                ),
            ),
        )

        assertEquals(null, viewModel.uiState.value.todayPlan)
        assertFalse(viewModel.uiState.value.canStartFirstTraining)
    }
}
