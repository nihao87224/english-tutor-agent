package cn.forever24.tutor.android

import cn.forever24.tutor.android.auth.AppLocale
import cn.forever24.tutor.android.auth.AuthenticatedUser
import cn.forever24.tutor.android.auth.AuthMode
import cn.forever24.tutor.android.auth.AuthRepository
import cn.forever24.tutor.android.auth.AuthSession
import cn.forever24.tutor.android.auth.InMemoryAuthSessionStore
import cn.forever24.tutor.android.network.ApiRequest
import cn.forever24.tutor.android.network.ApiResponse
import cn.forever24.tutor.android.network.ApiTransport
import cn.forever24.tutor.android.network.TutorApiClient
import cn.forever24.tutor.android.ui.CorrectionStyle
import cn.forever24.tutor.android.ui.AuthStatus
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
        val viewModel = viewModel()

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
        val viewModel = viewModel()

        viewModel.selectGoal(PrimaryGoal.WORKPLACE)

        assertEquals(PrimaryGoal.WORKPLACE, viewModel.uiState.value.selectedGoal)
        assertTrue(viewModel.uiState.value.canContinue)
        assertTrue(viewModel.canSubmitGoal())
    }

    @Test
    fun selectingAnotherGoalKeepsOnlyOnePrimaryGoal() {
        val viewModel = viewModel()

        viewModel.selectGoal(PrimaryGoal.WORKPLACE)
        viewModel.selectGoal(PrimaryGoal.IELTS)

        assertEquals(PrimaryGoal.IELTS, viewModel.uiState.value.selectedGoal)
    }

    @Test
    fun preferenceStateCanBeUpdatedAfterGoalSelection() {
        val viewModel = viewModel()

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
        val viewModel = viewModel()

        viewModel.selectDailyMinutes(17)

        assertEquals(20, viewModel.uiState.value.dailyMinutes)
    }

    @Test
    fun appliesRecoveredSelfAssessmentProgress() {
        val viewModel = viewModel()

        viewModel.applyOnboardingProgress("SELF_ASSESSMENT", completed = false, assessmentId = null)

        assertEquals(OnboardingStep.SELF_ASSESSMENT, viewModel.uiState.value.currentOnboardingStep)
        assertFalse(viewModel.uiState.value.onboardingCompleted)
    }

    @Test
    fun preservesAssessmentIdOnlyForAssessmentStep() {
        val viewModel = viewModel()

        viewModel.applyOnboardingProgress("ASSESSMENT", completed = false, assessmentId = "assessment-1")
        assertEquals("assessment-1", viewModel.uiState.value.assessmentId)

        viewModel.applyOnboardingProgress("RESULT", completed = false, assessmentId = "assessment-1")
        assertEquals(null, viewModel.uiState.value.assessmentId)
    }

    @Test
    fun appliesAssessmentSessionState() {
        val viewModel = viewModel()

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
        val viewModel = viewModel()

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
        val viewModel = viewModel()

        viewModel.applyAssessmentAnswerReceipt(answerId = "answer-1", accepted = true)

        assertEquals("answer-1", viewModel.uiState.value.latestAssessmentAnswerId)
        assertTrue(viewModel.uiState.value.latestAssessmentAnswerAccepted)
    }

    @Test
    fun ignoresBlankAssessmentAnswerReceipt() {
        val viewModel = viewModel()

        viewModel.applyAssessmentAnswerReceipt(answerId = " ", accepted = true)

        assertEquals(null, viewModel.uiState.value.latestAssessmentAnswerId)
        assertFalse(viewModel.uiState.value.latestAssessmentAnswerAccepted)
    }

    @Test
    fun appliesOpenAnswerEvaluation() {
        val viewModel = viewModel()

        viewModel.applyOpenAnswerEvaluation(
            feedback = "Clear response with a reason.",
            scorePercent = 76,
        )

        assertEquals("Clear response with a reason.", viewModel.uiState.value.latestOpenAnswerFeedback)
        assertEquals(76, viewModel.uiState.value.latestOpenAnswerScorePercent)
    }

    @Test
    fun ignoresInvalidOpenAnswerEvaluation() {
        val viewModel = viewModel()

        viewModel.applyOpenAnswerEvaluation(feedback = " ", scorePercent = 101)

        assertEquals(null, viewModel.uiState.value.latestOpenAnswerFeedback)
        assertEquals(null, viewModel.uiState.value.latestOpenAnswerScorePercent)
    }

    @Test
    fun completedFlagOnlyAppliesToCompleteStep() {
        val viewModel = viewModel()

        viewModel.applyOnboardingProgress("RESULT", completed = true, assessmentId = null)
        assertFalse(viewModel.uiState.value.onboardingCompleted)

        viewModel.applyOnboardingProgress("COMPLETE", completed = true, assessmentId = null)
        assertTrue(viewModel.uiState.value.onboardingCompleted)
    }

    @Test
    fun unknownRecoveredStepFallsBackToGoal() {
        val viewModel = viewModel()

        viewModel.applyOnboardingProgress("BROKEN", completed = false, assessmentId = null)

        assertEquals(OnboardingStep.GOAL, viewModel.uiState.value.currentOnboardingStep)
    }

    @Test
    fun selfAssessmentRequiresAllFourSkills() {
        val viewModel = viewModel()

        viewModel.selectSelfRating(SelfAssessmentSkill.LISTENING, SelfRating.INTERMEDIATE)
        viewModel.selectSelfRating(SelfAssessmentSkill.SPEAKING, SelfRating.BASIC)
        viewModel.selectSelfRating(SelfAssessmentSkill.READING, SelfRating.UPPER_INTERMEDIATE)

        assertFalse(viewModel.canSubmitSelfAssessment())

        viewModel.selectSelfRating(SelfAssessmentSkill.WRITING, SelfRating.INTERMEDIATE)

        assertTrue(viewModel.canSubmitSelfAssessment())
    }

    @Test
    fun selectingSelfRatingReplacesPreviousValueForSkill() {
        val viewModel = viewModel()

        viewModel.selectSelfRating(SelfAssessmentSkill.SPEAKING, SelfRating.BASIC)
        viewModel.selectSelfRating(SelfAssessmentSkill.SPEAKING, SelfRating.ADVANCED)

        assertEquals(SelfRating.ADVANCED, viewModel.uiState.value.selfRatings[SelfAssessmentSkill.SPEAKING])
    }

    @Test
    fun firstUseFlowReachesResultAndTodayPlan() {
        val viewModel = viewModel()

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
                    title = "Workplace quick response",
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
        val viewModel = viewModel()

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

    @Test
    fun invalidAuthInputShowsErrorWithoutCallingApi() {
        val transport = TestTransport()
        val viewModel = MainViewModel(repository(transport))

        viewModel.updateEmailInput("bad")
        viewModel.updatePasswordInput("short")
        viewModel.submitAuth()

        assertEquals("Enter a valid email and an 8+ character password.", viewModel.uiState.value.authError)
        assertEquals(0, transport.requests.size)
    }

    @Test
    fun loginStoresAuthenticatedAccountAndLoadsQuota() {
        val transport = TestTransport(
            ApiResponse(
                status = 200,
                headers = mapOf("Set-Cookie" to listOf("ETA_REFRESH_TOKEN=refresh-1; HttpOnly")),
                body = authBody(locale = "en"),
            ),
            ApiResponse(status = 200, body = quotaBody(used = 4, remaining = 46)),
            ApiResponse(status = 200, body = onboardingBody()),
            ApiResponse(status = 200, body = todayPlanBody()),
        )
        val viewModel = MainViewModel(repository(transport))

        viewModel.updateEmailInput("learner@example.com")
        viewModel.updatePasswordInput("password-1")
        viewModel.submitAuth()
        waitUntil {
            viewModel.uiState.value.authStatus == AuthStatus.AUTHENTICATED &&
                    viewModel.uiState.value.quota?.remaining == 46 &&
                    viewModel.uiState.value.todayPlan?.planId == "plan-1"
        }

        val state = viewModel.uiState.value
        assertEquals("learner@example.com", state.authenticatedEmail)
        assertEquals(46, state.quota?.remaining)
        assertEquals("Improve one sentence", state.todayPlan?.tasks?.single()?.title)
        assertEquals("Bearer access-token", transport.requests.last().headers["Authorization"])
    }

    @Test
    fun registerUsesRegisterEndpointAndAppliesBackendLocale() {
        val transport = TestTransport(
            ApiResponse(
                status = 200,
                headers = mapOf("Set-Cookie" to listOf("ETA_REFRESH_TOKEN=refresh-1; HttpOnly")),
                body = authBody(locale = "zh-CN"),
            ),
            ApiResponse(status = 200, body = quotaBody(used = 0, remaining = 50)),
        )
        val viewModel = MainViewModel(repository(transport))

        viewModel.switchAuthMode(AuthMode.REGISTER)
        viewModel.updateEmailInput("learner@example.com")
        viewModel.updatePasswordInput("password-1")
        viewModel.submitAuth()
        waitUntil { viewModel.uiState.value.authStatus == AuthStatus.AUTHENTICATED }

        assertEquals("/api/v1/auth/register", transport.requests.first().path)
        assertEquals(AppLocale.ZH_CN, viewModel.uiState.value.locale)
    }

    @Test
    fun quotaExceededSetsLearnerFriendlyState() {
        val transport = TestTransport(
            ApiResponse(
                status = 200,
                headers = mapOf("Set-Cookie" to listOf("ETA_REFRESH_TOKEN=refresh-1; HttpOnly")),
                body = authBody(locale = "en"),
            ),
            ApiResponse(
                status = 429,
                body = """
                    {
                      "type":"https://english-tutor/errors/daily-quota-exceeded",
                      "title":"Daily quota exceeded",
                      "status":429,
                      "remaining":0
                    }
                """.trimIndent(),
            ),
        )
        val viewModel = MainViewModel(repository(transport))

        viewModel.updateEmailInput("learner@example.com")
        viewModel.updatePasswordInput("password-1")
        viewModel.submitAuth()
        waitUntil { viewModel.uiState.value.quotaExceeded }

        assertTrue(viewModel.uiState.value.quotaExceeded)
        assertEquals("Daily quota exceeded.", viewModel.uiState.value.quotaError)
    }

    @Test
    fun expiredStoredSessionReturnsToSignInWhenRefreshIsRejected() {
        val sessionStore = InMemoryAuthSessionStore(
            AuthSession(
                user = authenticatedUser(),
                accessToken = "expired-token",
                expiresAtEpochMillis = 1_000L,
                refreshToken = "revoked-refresh-token",
            ),
        )
        val viewModel = MainViewModel(
            repository(
                transport = TestTransport(ApiResponse(status = 401, body = "{\"title\":\"Unauthorized\"}")),
                sessionStore = sessionStore,
                clockMillis = { 2_000L },
            ),
        )

        waitUntil { viewModel.uiState.value.authError == "Session expired. Please sign in again." }

        assertEquals("Session expired. Please sign in again.", viewModel.uiState.value.authError)
        assertEquals(null, sessionStore.load())
    }

    @Test
    fun logoutClearsAccountButKeepsSelectedLocale() {
        val transport = TestTransport(
            ApiResponse(status = 200, headers = mapOf("Set-Cookie" to listOf("ETA_REFRESH_TOKEN=refresh-1")), body = authBody("en")),
            ApiResponse(status = 200, body = quotaBody(used = 0, remaining = 50)),
            ApiResponse(status = 200),
        )
        val viewModel = MainViewModel(repository(transport))

        viewModel.setLocale(AppLocale.ZH_CN)
        viewModel.updateEmailInput("learner@example.com")
        viewModel.updatePasswordInput("password-1")
        viewModel.submitAuth()
        waitUntil { viewModel.uiState.value.authStatus == AuthStatus.AUTHENTICATED }
        viewModel.setLocale(AppLocale.ZH_CN)
        viewModel.logout()
        waitUntil { viewModel.uiState.value.authStatus == AuthStatus.SIGNED_OUT }

        assertEquals(null, viewModel.uiState.value.authenticatedEmail)
        assertEquals(AppLocale.ZH_CN, viewModel.uiState.value.locale)
    }

    @Test
    fun logoutReturnsToSignInEvenWhenTheServerCannotBeReached() {
        val transport = TestTransport(
            ApiResponse(status = 200, headers = mapOf("Set-Cookie" to listOf("ETA_REFRESH_TOKEN=refresh-1")), body = authBody("en")),
            ApiResponse(status = 200, body = quotaBody(used = 0, remaining = 50)),
            ApiResponse(status = 200, body = onboardingBody()),
            ApiResponse(status = 200, body = todayPlanBody()),
            ApiResponse(status = 500, body = "{\"title\":\"Server error\"}"),
        )
        val viewModel = MainViewModel(repository(transport))

        viewModel.updateEmailInput("learner@example.com")
        viewModel.updatePasswordInput("password-1")
        viewModel.submitAuth()
        waitUntil { viewModel.uiState.value.todayPlan?.planId == "plan-1" }

        viewModel.logout()
        waitUntil { viewModel.uiState.value.authStatus == AuthStatus.SIGNED_OUT }

        assertEquals(null, viewModel.uiState.value.authenticatedEmail)
    }

    private fun viewModel(): MainViewModel =
        MainViewModel(repository(TestTransport()))

    private fun repository(
        transport: TestTransport,
        sessionStore: InMemoryAuthSessionStore = InMemoryAuthSessionStore(),
        clockMillis: () -> Long = { 1_000L },
    ): AuthRepository =
        AuthRepository(
            apiClient = TutorApiClient("http://api.test", transport),
            sessionStore = sessionStore,
            clockMillis = clockMillis,
        )

    private fun authenticatedUser(): AuthenticatedUser =
        AuthenticatedUser(
            userKey = "learner-user",
            email = "learner@example.com",
            status = "ACTIVE",
            roles = listOf("USER"),
            locale = "en",
            timezone = "UTC",
        )

    private fun waitUntil(assertion: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 2_000L
        while (System.currentTimeMillis() < deadline) {
            if (assertion()) {
                return
            }
            Thread.sleep(20L)
        }
        assertTrue("condition was not met before timeout", assertion())
    }

    private fun authBody(locale: String): String =
        """
            {
              "user":{
                "userKey":"learner-user",
                "email":"learner@example.com",
                "status":"ACTIVE",
                "roles":["USER"],
                "locale":"$locale",
                "timezone":"UTC"
              },
              "accessToken":"access-token",
              "expiresIn":3600
            }
        """.trimIndent()

    private fun quotaBody(used: Int, remaining: Int): String =
        """
            {
              "quotaDate":"2026-08-10",
              "dailyLimit":50,
              "used":$used,
              "bonus":0,
              "remaining":$remaining,
              "unlimited":false,
              "resetAt":"2026-08-11T00:00:00Z"
            }
        """.trimIndent()

    private fun onboardingBody(): String =
        """
            {
              "step":"COMPLETE",
              "completed":true,
              "assessmentId":null
            }
        """.trimIndent()

    private fun todayPlanBody(): String =
        """
            {
              "planId":"plan-1",
              "date":"2026-08-10",
              "totalMinutes":10,
              "reasons":["Same account plan"],
              "tasks":[{
                "taskId":"task-1",
                "type":"CONVERSATION",
                "title":"Improve one sentence",
                "durationMinutes":10,
                "skillFocus":["speaking"],
                "difficulty":"EASY",
                "reason":"Practice expression"
              }]
            }
        """.trimIndent()
}

private class TestTransport(
    private vararg val responses: ApiResponse,
) : ApiTransport {
    val requests = mutableListOf<ApiRequest>()
    private var nextResponse = 0

    override fun execute(request: ApiRequest): ApiResponse {
        requests += request
        return responses[nextResponse++]
    }
}
