package cn.forever24.tutor.android.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorApiClientTest {

    @Test
    fun loginParsesSessionAndRefreshCookie() {
        val transport = RecordingTransport(
            ApiResponse(
                status = 200,
                headers = mapOf("Set-Cookie" to listOf("ETA_REFRESH_TOKEN=refresh-token; Path=/api/v1/auth; HttpOnly")),
                body = authBody("access-token"),
            ),
        )
        val client = TutorApiClient("http://api.test", transport) { "fixed-key" }

        val response = client.login("learner@example.com", "password-1")

        assertEquals("learner@example.com", response.user.email)
        assertEquals("access-token", response.accessToken)
        assertEquals("refresh-token", response.refreshToken)
        assertEquals("/api/v1/auth/login", transport.requests.single().path)
        assertEquals("fixed-key", transport.requests.single().headers["Idempotency-Key"])
    }

    @Test
    fun quotaSendsBearerToken() {
        val transport = RecordingTransport(
            ApiResponse(
                status = 200,
                body = """
                    {
                      "quotaDate":"2026-08-10",
                      "dailyLimit":50,
                      "used":2,
                      "bonus":5,
                      "remaining":53,
                      "unlimited":false,
                      "resetAt":"2026-08-11T00:00:00Z"
                    }
                """.trimIndent(),
            ),
        )
        val client = TutorApiClient("http://api.test", transport)

        val quota = client.quota("access-token")

        assertEquals(2, quota.used)
        assertEquals("Bearer access-token", transport.requests.single().headers["Authorization"])
    }

    @Test
    fun todayPlanUsesAuthenticatedApiAndParsesTasks() {
        val transport = RecordingTransport(
            ApiResponse(
                status = 200,
                body = """
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
                """.trimIndent(),
            ),
        )
        val client = TutorApiClient("http://api.test", transport)

        val plan = client.todayPlan("access-token")

        assertEquals("plan-1", plan.planId)
        assertEquals("task-1", plan.tasks.single().taskId)
        assertEquals("Bearer access-token", transport.requests.single().headers["Authorization"])
    }


    @Test
    fun quotaExceededProblemIsMapped() {
        val transport = RecordingTransport(
            ApiResponse(
                status = 429,
                body = """
                    {
                      "type":"https://english-tutor/errors/daily-quota-exceeded",
                      "title":"Daily quota exceeded",
                      "status":429,
                      "detail":"Today's AI learning quota has been used up.",
                      "dailyLimit":1,
                      "used":1,
                      "remaining":0,
                      "resetAt":"2026-08-11T00:00:00Z"
                    }
                """.trimIndent(),
            ),
        )
        val client = TutorApiClient("http://api.test", transport)

        val exception = kotlin.runCatching { client.quota("access-token") }.exceptionOrNull() as TutorApiException

        assertTrue(exception.isQuotaExceeded)
        assertEquals(0, exception.problem?.remaining)
    }

    private fun authBody(accessToken: String): String =
        """
            {
              "user":{
                "userKey":"learner-user",
                "email":"learner@example.com",
                "status":"ACTIVE",
                "roles":["USER"],
                "locale":"en",
                "timezone":"UTC"
              },
              "accessToken":"$accessToken",
              "expiresIn":3600
            }
        """.trimIndent()
}

private class RecordingTransport(
    private vararg val responses: ApiResponse,
) : ApiTransport {
    val requests = mutableListOf<ApiRequest>()
    private var nextResponse = 0

    override fun execute(request: ApiRequest): ApiResponse {
        requests += request
        return responses[nextResponse++]
    }
}
