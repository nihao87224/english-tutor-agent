package cn.forever24.tutor.android

import androidx.lifecycle.ViewModel
import cn.forever24.tutor.android.auth.AppLocale
import cn.forever24.tutor.android.auth.AuthCredentials
import cn.forever24.tutor.android.auth.AuthMode
import cn.forever24.tutor.android.auth.AuthRepository
import cn.forever24.tutor.android.auth.AuthSession
import cn.forever24.tutor.android.network.TutorApiException
import cn.forever24.tutor.android.ui.CorrectionStyle
import cn.forever24.tutor.android.ui.AssessmentResultUiModel
import cn.forever24.tutor.android.ui.AuthStatus
import cn.forever24.tutor.android.ui.HomeUiState
import cn.forever24.tutor.android.ui.OnboardingStep
import cn.forever24.tutor.android.ui.PrimaryGoal
import cn.forever24.tutor.android.ui.QuotaLoadStatus
import cn.forever24.tutor.android.ui.QuotaUiModel
import cn.forever24.tutor.android.ui.SelfAssessmentSkill
import cn.forever24.tutor.android.ui.SelfRating
import cn.forever24.tutor.android.ui.SkillScoreUiModel
import cn.forever24.tutor.android.ui.TodayPlanTaskUiModel
import cn.forever24.tutor.android.ui.TodayPlanUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableUiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    init {
        restoreSession()
    }

    fun switchAuthMode(mode: AuthMode) {
        mutableUiState.update { state ->
            state.copy(authMode = mode, authError = null)
        }
    }

    fun updateEmailInput(email: String) {
        mutableUiState.update { state ->
            state.copy(emailInput = email, authError = null)
        }
    }

    fun updatePasswordInput(password: String) {
        mutableUiState.update { state ->
            state.copy(passwordInput = password, authError = null)
        }
    }

    fun setLocale(locale: AppLocale) {
        mutableUiState.update { state ->
            state.copy(locale = locale)
        }
    }

    fun restoreSession() {
        val stored = authRepository.loadStoredSession()
        if (stored == null) {
            mutableUiState.update { state -> state.copy(authStatus = AuthStatus.SIGNED_OUT) }
            return
        }
        applySession(stored)
        refreshAccountData()
    }

    fun submitAuth() {
        val state = mutableUiState.value
        val credentials = AuthCredentials(state.emailInput, state.passwordInput)
        if (!credentials.isValid || state.authStatus == AuthStatus.SUBMITTING) {
            mutableUiState.update { it.copy(authError = "Enter a valid email and an 8+ character password.") }
            return
        }
        mutableUiState.update { it.copy(authStatus = AuthStatus.SUBMITTING, authError = null) }
        workerScope.launch {
            runCatching {
                if (state.authMode == AuthMode.REGISTER) {
                    authRepository.register(credentials)
                } else {
                    authRepository.login(credentials)
                }
            }.onSuccess { session ->
                applySession(session)
                refreshAccountData()
            }.onFailure {
                mutableUiState.update { current ->
                    current.copy(authStatus = AuthStatus.SIGNED_OUT, authError = "Authentication failed.")
                }
            }
        }
    }

    fun refreshQuota() {
        if (!mutableUiState.value.isAuthenticated) {
            return
        }
        mutableUiState.update { state ->
            state.copy(quotaStatus = QuotaLoadStatus.LOADING, quotaError = null, quotaExceeded = false)
        }
        workerScope.launch {
            runCatching {
                authRepository.currentQuota()
            }.onSuccess { quota ->
                mutableUiState.update { state ->
                    state.copy(
                        quota = QuotaUiModel(
                            quotaDate = quota.quotaDate,
                            dailyLimit = quota.dailyLimit,
                            used = quota.used,
                            bonus = quota.bonus,
                            remaining = quota.remaining,
                            unlimited = quota.unlimited,
                            resetAt = quota.resetAt,
                        ),
                        quotaStatus = QuotaLoadStatus.IDLE,
                        quotaError = null,
                        quotaExceeded = false,
                    )
                }
            }.onFailure { failure ->
                applyQuotaFailure(failure)
            }
        }
    }

    private fun refreshAccountData() {
        if (!mutableUiState.value.isAuthenticated) {
            return
        }
        mutableUiState.update { state ->
            state.copy(quotaStatus = QuotaLoadStatus.LOADING, quotaError = null, quotaExceeded = false)
        }
        workerScope.launch {
            runCatching {
                authRepository.currentQuota()
            }.onSuccess { quota ->
                mutableUiState.update { state ->
                    state.copy(
                        quota = QuotaUiModel(
                            quotaDate = quota.quotaDate,
                            dailyLimit = quota.dailyLimit,
                            used = quota.used,
                            bonus = quota.bonus,
                            remaining = quota.remaining,
                            unlimited = quota.unlimited,
                            resetAt = quota.resetAt,
                        ),
                        quotaStatus = QuotaLoadStatus.IDLE,
                        quotaError = null,
                        quotaExceeded = false,
                    )
                }
            }.onFailure { failure ->
                applyQuotaFailure(failure)
            }
            runCatching {
                val progress = authRepository.onboardingProgress()
                val plan = authRepository.todayPlan()
                progress to plan
            }.onSuccess { (progress, plan) ->
                applyOnboardingProgress(progress.step, progress.completed, progress.assessmentId)
                applyTodayPlan(
                    planId = plan.planId,
                    date = plan.date,
                    totalMinutes = plan.totalMinutes,
                    reasons = plan.reasons,
                    tasks = plan.tasks.map { task ->
                        TodayPlanTaskUiModel(
                            taskId = task.taskId,
                            type = task.type,
                            title = task.title,
                            durationMinutes = task.durationMinutes,
                            skillFocus = task.skillFocus,
                            difficulty = task.difficulty,
                            reason = task.reason,
                        )
                    },
                )
            }.onFailure {
                resetToSignedOutIfSessionWasCleared()
            }
        }
    }

    fun logout() {
        workerScope.launch {
            runCatching { authRepository.logout() }
            mutableUiState.update {
                HomeUiState(locale = it.locale)
            }
        }
    }

    fun refreshLearningData() {
        if (!mutableUiState.value.isAuthenticated) {
            return
        }
        workerScope.launch {
            runCatching {
                val progress = authRepository.onboardingProgress()
                val plan = authRepository.todayPlan()
                progress to plan
            }.onSuccess { (progress, plan) ->
                applyOnboardingProgress(progress.step, progress.completed, progress.assessmentId)
                applyTodayPlan(
                    planId = plan.planId,
                    date = plan.date,
                    totalMinutes = plan.totalMinutes,
                    reasons = plan.reasons,
                    tasks = plan.tasks.map { task ->
                        TodayPlanTaskUiModel(
                            taskId = task.taskId,
                            type = task.type,
                            title = task.title,
                            durationMinutes = task.durationMinutes,
                            skillFocus = task.skillFocus,
                            difficulty = task.difficulty,
                            reason = task.reason,
                        )
                    },
                )
            }.onFailure {
                resetToSignedOutIfSessionWasCleared()
            }
        }
    }

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
        if (!mutableUiState.value.isAuthenticated || !canSubmitGoal() || !canSubmitPreferences()) {
            return
        }
        val state = mutableUiState.value
        workerScope.launch {
            runCatching {
                authRepository.saveLearnerPreferences(
                    goal = state.selectedGoal?.name.orEmpty(),
                    dailyMinutes = state.dailyMinutes,
                    correctionStyle = state.correctionStyle.name,
                    reminderEnabled = state.reminderEnabled,
                    saveRawText = state.saveRawText,
                    saveRawAudio = state.saveRawAudio,
                )
            }.onSuccess {
                mutableUiState.update { current ->
                    current.copy(
                        currentOnboardingStep = OnboardingStep.COMPLETE,
                        onboardingCompleted = true,
                    )
                }
                refreshQuota()
            }.onFailure { failure ->
                mutableUiState.update { current ->
                    current.copy(
                        quotaExceeded = (failure as? TutorApiException)?.isQuotaExceeded == true,
                        quotaError = if ((failure as? TutorApiException)?.isQuotaExceeded == true) {
                            "Daily quota exceeded."
                        } else {
                            "Save failed."
                        },
                    )
                }
            }
        }
    }

    override fun onCleared() {
        workerScope.cancel()
        super.onCleared()
    }

    private fun applySession(session: AuthSession) {
        mutableUiState.update { state ->
            state.copy(
                locale = AppLocale.fromBackend(session.user.locale),
                authStatus = AuthStatus.AUTHENTICATED,
                authenticatedEmail = session.user.email,
                authenticatedUserKey = session.user.userKey,
                authenticatedStatus = session.user.status,
                authenticatedRoles = session.user.roles,
                authError = null,
                emailInput = session.user.email,
                passwordInput = "",
            )
        }
    }

    private fun applyQuotaFailure(failure: Throwable) {
        if (resetToSignedOutIfSessionWasCleared()) {
            return
        }
        mutableUiState.update { state ->
            state.copy(
                quotaStatus = QuotaLoadStatus.ERROR,
                quotaError = if ((failure as? TutorApiException)?.isQuotaExceeded == true) {
                    "Daily quota exceeded."
                } else {
                    "Quota is unavailable."
                },
                quotaExceeded = (failure as? TutorApiException)?.isQuotaExceeded == true,
            )
        }
    }

    private fun resetToSignedOutIfSessionWasCleared(): Boolean {
        if (authRepository.loadStoredSession() != null) {
            return false
        }
        mutableUiState.update { state ->
            HomeUiState(
                locale = state.locale,
                authError = "Session expired. Please sign in again.",
            )
        }
        return true
    }
}
