package cn.forever24.tutor.android.ui

data class HomeUiState(
    val title: String = "Choose your learning goal",
    val subtitle: String = "Pick one focus. The plan will still adapt to your actual level.",
    val availableGoals: List<PrimaryGoal> = PrimaryGoal.entries,
    val availableDailyMinutes: List<Int> = listOf(5, 10, 20, 30, 45),
    val availableCorrectionStyles: List<CorrectionStyle> = CorrectionStyle.entries,
    val selectedGoal: PrimaryGoal? = null,
    val dailyMinutes: Int = 20,
    val correctionStyle: CorrectionStyle = CorrectionStyle.STANDARD,
    val reminderEnabled: Boolean = false,
    val saveRawText: Boolean = true,
    val saveRawAudio: Boolean = true,
    val currentOnboardingStep: OnboardingStep = OnboardingStep.GOAL,
    val onboardingCompleted: Boolean = false,
    val assessmentId: String? = null,
    val assessmentStatus: String? = null,
    val assessmentTargetMinutes: Int? = null,
    val assessmentEstimatedRemainingMinutes: Int? = null,
    val latestAssessmentAnswerId: String? = null,
    val latestAssessmentAnswerAccepted: Boolean = false,
    val latestOpenAnswerFeedback: String? = null,
    val latestOpenAnswerScorePercent: Int? = null,
    val assessmentResult: AssessmentResultUiModel? = null,
    val todayPlan: TodayPlanUiModel? = null,
    val selfRatings: Map<SelfAssessmentSkill, SelfRating?> = SelfAssessmentSkill.entries.associateWith { null },
    val canContinue: Boolean = false,
) {
    val selectedGoalName: String = selectedGoal?.label.orEmpty()
    val canSubmitPreferences: Boolean =
        selectedGoal != null &&
                dailyMinutes in availableDailyMinutes &&
                correctionStyle in availableCorrectionStyles
    val canSubmitSelfAssessment: Boolean =
        SelfAssessmentSkill.entries.all { skill -> selfRatings[skill] != null }
    val canStartFirstTraining: Boolean =
        currentOnboardingStep in setOf(OnboardingStep.RESULT, OnboardingStep.COMPLETE) &&
                assessmentResult != null &&
                todayPlan?.tasks?.isNotEmpty() == true
}

data class AssessmentResultUiModel(
    val assessmentId: String,
    val overallLevel: String,
    val confidencePercent: Int,
    val summary: String,
    val strengths: List<String>,
    val priorities: List<String>,
    val skills: List<SkillScoreUiModel>,
)

data class SkillScoreUiModel(
    val skill: String,
    val scorePercent: Int,
    val level: String,
    val confidencePercent: Int,
)

data class TodayPlanUiModel(
    val planId: String,
    val date: String,
    val totalMinutes: Int,
    val reasons: List<String>,
    val tasks: List<TodayPlanTaskUiModel>,
)

data class TodayPlanTaskUiModel(
    val taskId: String,
    val type: String,
    val title: String,
    val durationMinutes: Int,
    val skillFocus: List<String>,
    val difficulty: String,
    val reason: String,
)
