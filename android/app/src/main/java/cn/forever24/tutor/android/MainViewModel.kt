package cn.forever24.tutor.android

import androidx.lifecycle.ViewModel
import cn.forever24.tutor.android.ui.CorrectionStyle
import cn.forever24.tutor.android.ui.AssessmentResultUiModel
import cn.forever24.tutor.android.ui.HomeUiState
import cn.forever24.tutor.android.ui.OnboardingStep
import cn.forever24.tutor.android.ui.PrimaryGoal
import cn.forever24.tutor.android.ui.SelfAssessmentSkill
import cn.forever24.tutor.android.ui.SelfRating
import cn.forever24.tutor.android.ui.SkillScoreUiModel
import cn.forever24.tutor.android.ui.TodayPlanTaskUiModel
import cn.forever24.tutor.android.ui.TodayPlanUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private val mutableUiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    fun selectGoal(goal: PrimaryGoal) {
        mutableUiState.update { state ->
            state.copy(
                selectedGoal = goal,
                canContinue = true,
            )
        }
    }

    fun canSubmitGoal(): Boolean = mutableUiState.value.selectedGoal != null

    fun selectDailyMinutes(minutes: Int) {
        mutableUiState.update { state ->
            if (minutes !in state.availableDailyMinutes) {
                state
            } else {
                state.copy(dailyMinutes = minutes)
            }
        }
    }

    fun selectCorrectionStyle(style: CorrectionStyle) {
        mutableUiState.update { state ->
            state.copy(correctionStyle = style)
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        mutableUiState.update { state ->
            state.copy(reminderEnabled = enabled)
        }
    }

    fun setSaveRawText(enabled: Boolean) {
        mutableUiState.update { state ->
            state.copy(saveRawText = enabled)
        }
    }

    fun setSaveRawAudio(enabled: Boolean) {
        mutableUiState.update { state ->
            state.copy(saveRawAudio = enabled)
        }
    }

    fun applyOnboardingProgress(
        stepValue: String?,
        completed: Boolean,
        assessmentId: String?,
    ) {
        val step = OnboardingStep.fromBackendValue(stepValue)
        mutableUiState.update { state ->
            state.copy(
                currentOnboardingStep = step,
                onboardingCompleted = completed && step == OnboardingStep.COMPLETE,
                assessmentId = if (step == OnboardingStep.ASSESSMENT) assessmentId else null,
            )
        }
    }

    fun canSubmitPreferences(): Boolean = mutableUiState.value.canSubmitPreferences

    fun applyAssessmentSession(
        assessmentId: String,
        status: String,
        targetMinutes: Int,
        estimatedRemainingMinutes: Int?,
    ) {
        if (assessmentId.isBlank() || targetMinutes !in 5..15) {
            return
        }
        mutableUiState.update { state ->
            state.copy(
                currentOnboardingStep = OnboardingStep.ASSESSMENT,
                assessmentId = assessmentId,
                assessmentStatus = status,
                assessmentTargetMinutes = targetMinutes,
                assessmentEstimatedRemainingMinutes = estimatedRemainingMinutes,
            )
        }
    }

    fun applyAssessmentAnswerReceipt(answerId: String, accepted: Boolean) {
        if (answerId.isBlank()) {
            return
        }
        mutableUiState.update { state ->
            state.copy(
                latestAssessmentAnswerId = answerId,
                latestAssessmentAnswerAccepted = accepted,
            )
        }
    }

    fun applyOpenAnswerEvaluation(feedback: String, scorePercent: Int) {
        if (feedback.isBlank() || scorePercent !in 0..100) {
            return
        }
        mutableUiState.update { state ->
            state.copy(
                latestOpenAnswerFeedback = feedback.trim(),
                latestOpenAnswerScorePercent = scorePercent,
            )
        }
    }

    fun applyAssessmentCompletion(assessmentId: String, status: String) {
        if (assessmentId.isBlank() || status != "COMPLETED") {
            return
        }
        mutableUiState.update { state ->
            state.copy(
                currentOnboardingStep = OnboardingStep.RESULT,
                assessmentId = null,
                assessmentStatus = status,
                assessmentEstimatedRemainingMinutes = 0,
            )
        }
    }

    fun applyAssessmentResult(
        assessmentId: String,
        overallLevel: String,
        confidencePercent: Int,
        summary: String,
        strengths: List<String>,
        priorities: List<String>,
        skills: List<SkillScoreUiModel>,
    ) {
        if (assessmentId.isBlank() ||
            overallLevel.isBlank() ||
            summary.isBlank() ||
            confidencePercent !in 0..100 ||
            priorities.isEmpty() ||
            skills.isEmpty()
        ) {
            return
        }
        mutableUiState.update { state ->
            state.copy(
                currentOnboardingStep = OnboardingStep.RESULT,
                onboardingCompleted = false,
                assessmentId = null,
                assessmentResult = AssessmentResultUiModel(
                    assessmentId = assessmentId,
                    overallLevel = overallLevel,
                    confidencePercent = confidencePercent,
                    summary = summary.trim(),
                    strengths = strengths.filter { it.isNotBlank() }.take(5),
                    priorities = priorities.filter { it.isNotBlank() }.take(5),
                    skills = skills.filter { skill ->
                        skill.skill.isNotBlank() &&
                                skill.level.isNotBlank() &&
                                skill.scorePercent in 0..100 &&
                                skill.confidencePercent in 0..100
                    },
                ),
            )
        }
    }

    fun applyTodayPlan(
        planId: String,
        date: String,
        totalMinutes: Int,
        reasons: List<String>,
        tasks: List<TodayPlanTaskUiModel>,
    ) {
        val validTasks = tasks.filter { task ->
            task.taskId.isNotBlank() &&
                    task.type.isNotBlank() &&
                    task.title.isNotBlank() &&
                    task.durationMinutes > 0 &&
                    task.skillFocus.isNotEmpty() &&
                    task.difficulty.isNotBlank() &&
                    task.reason.isNotBlank()
        }
        if (planId.isBlank() ||
            date.isBlank() ||
            totalMinutes <= 0 ||
            reasons.none { it.isNotBlank() } ||
            validTasks.isEmpty() ||
            validTasks.sumOf { it.durationMinutes } != totalMinutes
        ) {
            return
        }
        mutableUiState.update { state ->
            state.copy(
                todayPlan = TodayPlanUiModel(
                    planId = planId,
                    date = date,
                    totalMinutes = totalMinutes,
                    reasons = reasons.filter { it.isNotBlank() }.take(3),
                    tasks = validTasks.take(3),
                ),
            )
        }
    }

    fun selectSelfRating(skill: SelfAssessmentSkill, rating: SelfRating) {
        mutableUiState.update { state ->
            state.copy(selfRatings = state.selfRatings + (skill to rating))
        }
    }

    fun canSubmitSelfAssessment(): Boolean = mutableUiState.value.canSubmitSelfAssessment

    fun continueWithSelectedGoal() {
        if (!canSubmitGoal()) {
            return
        }
    }
}
