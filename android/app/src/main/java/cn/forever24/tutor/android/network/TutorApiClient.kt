package cn.forever24.tutor.android.network

import cn.forever24.tutor.android.auth.AuthenticatedUser
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class TutorApiClient(
    private val baseUrl: String,
    private val transport: ApiTransport = HttpUrlConnectionTransport(baseUrl),
    private val idempotencyKeyFactory: () -> String = { UUID.randomUUID().toString() },
) {
    fun register(email: String, password: String): AuthApiResponse =
        auth("/api/v1/auth/register", email, password)

    fun login(email: String, password: String): AuthApiResponse =
        auth("/api/v1/auth/login", email, password)

    fun refresh(refreshToken: String?): AuthApiResponse =
        parseAuthResponse(
            executeJson(
                ApiRequest(
                    method = "POST",
                    path = "/api/v1/auth/refresh",
                    headers = jsonHeaders(),
                    body = JSONObject().put("refreshToken", refreshToken).toString(),
                ),
            ),
        )

    fun logout(refreshToken: String?) {
        executeJson(
            ApiRequest(
                method = "POST",
                path = "/api/v1/auth/logout",
                headers = jsonHeaders(idempotent = true),
                body = JSONObject().put("refreshToken", refreshToken).toString(),
            ),
        )
    }

    fun me(accessToken: String): AuthenticatedUser =
        parseUser(
            JSONObject(
                executeJson(
                    ApiRequest(
                        method = "GET",
                        path = "/api/v1/me",
                        headers = bearerHeaders(accessToken),
                    ),
                ).body,
            ),
        )

    fun quota(accessToken: String): QuotaStatus =
        parseQuota(
            JSONObject(
                executeJson(
                    ApiRequest(
                        method = "GET",
                        path = "/api/v1/me/quota",
                        headers = bearerHeaders(accessToken),
                    ),
                ).body,
            ),
        )

    fun onboardingProgress(accessToken: String): OnboardingProgressStatus =
        parseOnboardingProgress(
            JSONObject(
                executeJson(
                    ApiRequest(
                        method = "GET",
                        path = "/api/v1/onboarding/progress",
                        headers = bearerHeaders(accessToken),
                    ),
                ).body,
            ),
        )

    fun todayPlan(accessToken: String): LearningPlanStatus =
        parseLearningPlan(
            JSONObject(
                executeJson(
                    ApiRequest(
                        method = "GET",
                        path = "/api/v1/plans/today",
                        headers = bearerHeaders(accessToken),
                    ),
                ).body,
            ),
        )

    fun putPrimaryGoal(accessToken: String, goal: String) {
        executeJson(
            ApiRequest(
                method = "PUT",
                path = "/api/v1/profile/primary-goal",
                headers = bearerHeaders(accessToken, idempotent = true),
                body = JSONObject().put("goal", goal).toString(),
            ),
        )
    }

    fun putPreferences(
        accessToken: String,
        dailyMinutes: Int,
        correctionStyle: String,
        reminderEnabled: Boolean,
        saveRawText: Boolean,
        saveRawAudio: Boolean,
    ) {
        executeJson(
            ApiRequest(
                method = "PUT",
                path = "/api/v1/profile/preferences",
                headers = bearerHeaders(accessToken, idempotent = true),
                body = JSONObject()
                    .put("dailyMinutes", dailyMinutes)
                    .put("correctionStyle", correctionStyle)
                    .put("reminderEnabled", reminderEnabled)
                    .put("saveRawText", saveRawText)
                    .put("saveRawAudio", saveRawAudio)
                    .toString(),
            ),
        )
    }

    private fun auth(path: String, email: String, password: String): AuthApiResponse =
        parseAuthResponse(
            executeJson(
                ApiRequest(
                    method = "POST",
                    path = path,
                    headers = jsonHeaders(idempotent = true),
                    body = JSONObject()
                        .put("email", email.trim())
                        .put("password", password)
                        .toString(),
                ),
            ),
        )

    private fun executeJson(request: ApiRequest): ApiResponse {
        val response = transport.execute(request)
        if (!response.isSuccessful) {
            throw TutorApiException(response.status, parseProblem(response.body, response.status))
        }
        return response
    }

    private fun parseAuthResponse(response: ApiResponse): AuthApiResponse {
        val json = JSONObject(response.body)
        return AuthApiResponse(
            user = parseUser(json.getJSONObject("user")),
            accessToken = json.getString("accessToken"),
            expiresInSeconds = json.getLong("expiresIn"),
            refreshToken = refreshTokenFrom(response.headers),
        )
    }

    private fun parseUser(json: JSONObject): AuthenticatedUser =
        AuthenticatedUser(
            userKey = json.getString("userKey"),
            email = json.getString("email"),
            status = json.getString("status"),
            roles = json.optJSONArray("roles").toStringList(),
            locale = json.optString("locale", "en"),
            timezone = json.optString("timezone", "UTC"),
        )

    private fun parseQuota(json: JSONObject): QuotaStatus =
        QuotaStatus(
            quotaDate = json.getString("quotaDate"),
            dailyLimit = json.getInt("dailyLimit"),
            used = json.getInt("used"),
            bonus = json.getInt("bonus"),
            remaining = json.getInt("remaining"),
            unlimited = json.getBoolean("unlimited"),
            resetAt = json.getString("resetAt"),
        )

    private fun parseOnboardingProgress(json: JSONObject): OnboardingProgressStatus =
        OnboardingProgressStatus(
            step = json.getString("step"),
            completed = json.getBoolean("completed"),
            assessmentId = json.optStringOrNull("assessmentId"),
        )

    private fun parseLearningPlan(json: JSONObject): LearningPlanStatus =
        LearningPlanStatus(
            planId = json.getString("planId"),
            date = json.getString("date"),
            totalMinutes = json.getInt("totalMinutes"),
            reasons = json.optJSONArray("reasons").toStringList(),
            tasks = json.getJSONArray("tasks").toPlanTasks(),
        )

    private fun parseProblem(body: String, fallbackStatus: Int): ApiProblem? {
        if (body.isBlank()) {
            return null
        }
        return try {
            val json = JSONObject(body)
            ApiProblem(
                type = json.optString("type", "about:blank"),
                title = json.optString("title", "Request failed"),
                status = json.optInt("status", fallbackStatus),
                detail = json.optStringOrNull("detail"),
                code = json.optStringOrNull("code"),
                dailyLimit = json.optIntOrNull("dailyLimit"),
                used = json.optIntOrNull("used"),
                remaining = json.optIntOrNull("remaining"),
                resetAt = json.optStringOrNull("resetAt"),
            )
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun jsonHeaders(idempotent: Boolean = false): Map<String, String> =
        buildMap {
            put("Content-Type", "application/json")
            if (idempotent) {
                put("Idempotency-Key", idempotencyKeyFactory())
            }
        }

    private fun bearerHeaders(accessToken: String, idempotent: Boolean = false): Map<String, String> =
        jsonHeaders(idempotent) + mapOf("Authorization" to "Bearer $accessToken")

    private fun refreshTokenFrom(headers: Map<String, List<String>>): String? {
        val cookies = headers.entries
            .filter { (key, _) -> key.equals("Set-Cookie", ignoreCase = true) }
            .flatMap { it.value }
        return cookies
            .firstNotNullOfOrNull { cookie ->
                cookie.split(";")
                    .map { it.trim() }
                    .firstOrNull { it.startsWith("ETA_REFRESH_TOKEN=") }
                    ?.substringAfter("=")
                    ?.takeIf { it.isNotBlank() }
            }
    }
}

class HttpUrlConnectionTransport(private val baseUrl: String) : ApiTransport {
    override fun execute(request: ApiRequest): ApiResponse {
        val connection = (URL(baseUrl.trimEnd('/') + request.path).openConnection() as HttpURLConnection)
        connection.requestMethod = request.method
        connection.connectTimeout = 10_000
        connection.readTimeout = 20_000
        request.headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        if (request.body != null) {
            connection.doOutput = true
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(request.body)
            }
        }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        return ApiResponse(
            status = status,
            headers = connection.headerFields
                .filterKeys { it != null }
                .mapKeys { it.key.orEmpty() },
            body = body,
        )
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) {
        return emptyList()
    }
    return (0 until length()).mapNotNull { index -> optString(index).takeIf { it.isNotBlank() } }
}

private fun JSONArray.toPlanTasks(): List<LearningPlanTaskStatus> =
    (0 until length()).map { index ->
        val task = getJSONObject(index)
        LearningPlanTaskStatus(
            taskId = task.getString("taskId"),
            type = task.getString("type"),
            title = task.getString("title"),
            durationMinutes = task.getInt("durationMinutes"),
            skillFocus = task.optJSONArray("skillFocus").toStringList(),
            difficulty = task.getString("difficulty"),
            reason = task.optString("reason", ""),
        )
    }

private fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

private fun JSONObject.optIntOrNull(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null
