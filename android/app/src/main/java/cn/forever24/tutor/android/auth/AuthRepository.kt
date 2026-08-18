package cn.forever24.tutor.android.auth

import cn.forever24.tutor.android.network.AuthApiResponse
import cn.forever24.tutor.android.network.LearningPlanStatus
import cn.forever24.tutor.android.network.OnboardingProgressStatus
import cn.forever24.tutor.android.network.QuotaStatus
import cn.forever24.tutor.android.network.TutorApiClient
import cn.forever24.tutor.android.network.TutorApiException

class AuthRepository(
    private val apiClient: TutorApiClient,
    private val sessionStore: AuthSessionStore,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
) {
    fun loadStoredSession(): AuthSession? = sessionStore.load()

    fun login(credentials: AuthCredentials): AuthSession {
        require(credentials.isValid) { "email and password are required" }
        return save(apiClient.login(credentials.email, credentials.password))
    }

    fun register(credentials: AuthCredentials): AuthSession {
        require(credentials.isValid) { "email and password are required" }
        return save(apiClient.register(credentials.email, credentials.password))
    }

    fun refresh(): AuthSession? {
        val existing = sessionStore.load() ?: return null
        return try {
            save(apiClient.refresh(existing.refreshToken ?: existing.accessToken))
        } catch (_: TutorApiException) {
            sessionStore.clear()
            null
        }
    }

    fun currentQuota(): QuotaStatus =
        withFreshSession { session -> apiClient.quota(session.accessToken) }

    fun onboardingProgress(): OnboardingProgressStatus =
        withFreshSession { session -> apiClient.onboardingProgress(session.accessToken) }

    fun todayPlan(): LearningPlanStatus =
        withFreshSession { session -> apiClient.todayPlan(session.accessToken) }

    fun saveLearnerPreferences(
        goal: String,
        dailyMinutes: Int,
        correctionStyle: String,
        reminderEnabled: Boolean,
        saveRawText: Boolean,
        saveRawAudio: Boolean,
    ) {
        withFreshSession { session ->
            apiClient.putPrimaryGoal(session.accessToken, goal)
            apiClient.putPreferences(
                accessToken = session.accessToken,
                dailyMinutes = dailyMinutes,
                correctionStyle = correctionStyle,
                reminderEnabled = reminderEnabled,
                saveRawText = saveRawText,
                saveRawAudio = saveRawAudio,
            )
        }
    }

    fun logout() {
        val refreshToken = sessionStore.load()?.refreshToken
        try {
            apiClient.logout(refreshToken)
        } finally {
            sessionStore.clear()
        }
    }

    fun clear() {
        sessionStore.clear()
    }

    private fun save(response: AuthApiResponse): AuthSession {
        val session = AuthSession(
            user = response.user,
            accessToken = response.accessToken,
            expiresAtEpochMillis = clockMillis() + response.expiresInSeconds * 1000L,
            refreshToken = response.refreshToken,
        )
        sessionStore.save(session)
        return session
    }

    private fun <T> withFreshSession(block: (AuthSession) -> T): T {
        val session = sessionStore.load() ?: throw IllegalStateException("authentication is required")
        val freshSession = if (session.expiresAtEpochMillis <= clockMillis() + REFRESH_SKEW_MILLIS) {
            refresh() ?: throw IllegalStateException("authentication is required")
        } else {
            session
        }
        return try {
            block(freshSession)
        } catch (exception: TutorApiException) {
            if (exception.status != 401) {
                throw exception
            }
            val refreshed = refresh() ?: throw exception
            block(refreshed)
        }
    }

    private companion object {
        const val REFRESH_SKEW_MILLIS = 30_000L
    }
}
