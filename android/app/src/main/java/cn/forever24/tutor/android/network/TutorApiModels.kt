package cn.forever24.tutor.android.network

import cn.forever24.tutor.android.auth.AuthenticatedUser

data class AuthApiResponse(
    val user: AuthenticatedUser,
    val accessToken: String,
    val expiresInSeconds: Long,
    val refreshToken: String?,
)

data class QuotaStatus(
    val quotaDate: String,
    val dailyLimit: Int,
    val used: Int,
    val bonus: Int,
    val remaining: Int,
    val unlimited: Boolean,
    val resetAt: String,
)

data class OnboardingProgressStatus(
    val step: String,
    val completed: Boolean,
    val assessmentId: String?,
)

data class LearningPlanStatus(
    val planId: String,
    val date: String,
    val totalMinutes: Int,
    val reasons: List<String>,
    val tasks: List<LearningPlanTaskStatus>,
)

data class LearningPlanTaskStatus(
    val taskId: String,
    val type: String,
    val title: String,
    val durationMinutes: Int,
    val skillFocus: List<String>,
    val difficulty: String,
    val reason: String,
)

data class ApiProblem(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String?,
    val code: String?,
    val dailyLimit: Int?,
    val used: Int?,
    val remaining: Int?,
    val resetAt: String?,
)

class TutorApiException(
    val status: Int,
    val problem: ApiProblem?,
) : RuntimeException(problem?.detail ?: problem?.title ?: "Request failed with HTTP $status") {
    val isQuotaExceeded: Boolean =
        status == 429 || problem?.type?.contains("daily-quota-exceeded") == true
}

data class ApiRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
)

data class ApiResponse(
    val status: Int,
    val headers: Map<String, List<String>> = emptyMap(),
    val body: String = "",
) {
    val isSuccessful: Boolean = status in 200..299
}

fun interface ApiTransport {
    fun execute(request: ApiRequest): ApiResponse
}
