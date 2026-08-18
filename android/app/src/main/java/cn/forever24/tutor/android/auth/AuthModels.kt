package cn.forever24.tutor.android.auth

data class AuthenticatedUser(
    val userKey: String,
    val email: String,
    val status: String,
    val roles: List<String>,
    val locale: String,
    val timezone: String,
)

data class AuthSession(
    val user: AuthenticatedUser,
    val accessToken: String,
    val expiresAtEpochMillis: Long,
    val refreshToken: String?,
)

data class AuthCredentials(
    val email: String,
    val password: String,
) {
    val isValid: Boolean =
        email.trim().contains("@") && password.length >= 8
}

enum class AuthMode {
    LOGIN,
    REGISTER,
}

enum class AppLocale(val backendValue: String) {
    EN("en"),
    ZH_CN("zh-CN");

    companion object {
        fun fromBackend(value: String?): AppLocale =
            if (value?.lowercase()?.startsWith("zh") == true) ZH_CN else EN
    }
}
