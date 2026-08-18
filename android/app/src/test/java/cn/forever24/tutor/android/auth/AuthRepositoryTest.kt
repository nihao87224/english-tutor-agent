package cn.forever24.tutor.android.auth

import cn.forever24.tutor.android.network.ApiRequest
import cn.forever24.tutor.android.network.ApiResponse
import cn.forever24.tutor.android.network.ApiTransport
import cn.forever24.tutor.android.network.TutorApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthRepositoryTest {

    @Test
    fun loginStoresSessionWithRefreshTokenFromCookie() {
        val store = InMemoryAuthSessionStore()
        val repository = AuthRepository(
            apiClient = TutorApiClient(
                baseUrl = "http://api.test",
                transport = QueueTransport(
                    ApiResponse(
                        status = 200,
                        headers = mapOf("Set-Cookie" to listOf("ETA_REFRESH_TOKEN=refresh-1; HttpOnly")),
                        body = authBody("access-1"),
                    ),
                ),
            ),
            sessionStore = store,
            clockMillis = { 1_000L },
        )

        repository.login(AuthCredentials("learner@example.com", "password-1"))

        assertEquals("access-1", store.load()?.accessToken)
        assertEquals("refresh-1", store.load()?.refreshToken)
        assertEquals(3_601_000L, store.load()?.expiresAtEpochMillis)
    }

    @Test
    fun expiredSessionRefreshesBeforeQuotaRequest() {
        val store = InMemoryAuthSessionStore(
            AuthSession(
                user = user(),
                accessToken = "expired",
                expiresAtEpochMillis = 1_000L,
                refreshToken = "refresh-1",
            ),
        )
        val transport = QueueTransport(
            ApiResponse(
                status = 200,
                headers = mapOf("Set-Cookie" to listOf("ETA_REFRESH_TOKEN=refresh-2; HttpOnly")),
                body = authBody("access-2"),
            ),
            ApiResponse(
                status = 200,
                body = quotaBody(used = 3),
            ),
        )
        val repository = AuthRepository(TutorApiClient("http://api.test", transport), store, clockMillis = { 2_000L })

        val quota = repository.currentQuota()

        assertEquals(3, quota.used)
        assertEquals("access-2", store.load()?.accessToken)
        assertEquals("/api/v1/auth/refresh", transport.requests[0].path)
        assertEquals("/api/v1/me/quota", transport.requests[1].path)
        assertEquals("Bearer access-2", transport.requests[1].headers["Authorization"])
    }

    @Test
    fun logoutClearsStoredSession() {
        val store = InMemoryAuthSessionStore(
            AuthSession(user(), "access", 10_000L, "refresh"),
        )
        val repository = AuthRepository(
            TutorApiClient("http://api.test", QueueTransport(ApiResponse(status = 200))),
            store,
        )

        repository.logout()

        assertNull(store.load())
    }

    private fun user(): AuthenticatedUser =
        AuthenticatedUser(
            userKey = "learner-user",
            email = "learner@example.com",
            status = "ACTIVE",
            roles = listOf("USER"),
            locale = "en",
            timezone = "UTC",
        )

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

    private fun quotaBody(used: Int): String =
        """
            {
              "quotaDate":"2026-08-10",
              "dailyLimit":50,
              "used":$used,
              "bonus":0,
              "remaining":${50 - used},
              "unlimited":false,
              "resetAt":"2026-08-11T00:00:00Z"
            }
        """.trimIndent()
}

private class QueueTransport(
    private vararg val responses: ApiResponse,
) : ApiTransport {
    val requests = mutableListOf<ApiRequest>()
    private var nextResponse = 0

    override fun execute(request: ApiRequest): ApiResponse {
        requests += request
        return responses[nextResponse++]
    }
}
