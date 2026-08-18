package cn.forever24.tutor.android.ui

import cn.forever24.tutor.android.auth.AppLocale
import cn.forever24.tutor.android.auth.AuthMode

data class HomeUiState(
    val locale: AppLocale = AppLocale.EN,
    val authMode: AuthMode = AuthMode.LOGIN,
    val authStatus: AuthStatus = AuthStatus.SIGNED_OUT,
    val emailInput: String = "",
    val passwordInput: String = "",
    val authenticatedEmail: String? = null,
    val authenticatedUserKey: String? = null,
    val authenticatedStatus: String? = null,
    val authenticatedRoles: List<String> = emptyList(),
    val authError: String? = null,
    val quota: QuotaUiModel? = null,
    val quotaStatus: QuotaLoadStatus = QuotaLoadStatus.IDLE,
    val quotaError: String? = null,
    val quotaExceeded: Boolean = false,
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
    val isAuthenticated: Boolean = authStatus == AuthStatus.AUTHENTICATED
    val isAuthFormValid: Boolean =
        emailInput.trim().contains("@") && passwordInput.length >= 8
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

enum class AuthStatus {
    CHECKING_SESSION,
    SIGNED_OUT,
    SUBMITTING,
    AUTHENTICATED,
}

enum class QuotaLoadStatus {
    IDLE,
    LOADING,
    ERROR,
}

data class QuotaUiModel(
    val quotaDate: String,
    val dailyLimit: Int,
    val used: Int,
    val bonus: Int,
    val remaining: Int,
    val unlimited: Boolean,
    val resetAt: String,
) {
    val usageLabel: String =
        if (unlimited) {
            "Unlimited"
        } else {
            "$used / ${dailyLimit + bonus} used"
        }
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
