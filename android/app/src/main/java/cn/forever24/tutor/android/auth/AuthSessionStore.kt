package cn.forever24.tutor.android.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface AuthSessionStore {
    fun load(): AuthSession?
    fun save(session: AuthSession)
    fun clear()
}

class SharedPreferencesAuthSessionStore(context: Context) : AuthSessionStore {
    private val preferences = context.getSharedPreferences("english_tutor_auth_session", Context.MODE_PRIVATE)

    override fun load(): AuthSession? {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null)?.let(::decrypt)?.takeIf { it.isNotBlank() } ?: return null
        val email = preferences.getString(KEY_EMAIL, null)?.takeIf { it.isNotBlank() } ?: return null
        val userKey = preferences.getString(KEY_USER_KEY, null)?.takeIf { it.isNotBlank() } ?: return null
        val expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L).takeIf { it > 0L } ?: return null
        return AuthSession(
            user = AuthenticatedUser(
                userKey = userKey,
                email = email,
                status = preferences.getString(KEY_STATUS, "ACTIVE").orEmpty(),
                roles = preferences.getStringSet(KEY_ROLES, emptySet()).orEmpty().toList().sorted(),
                locale = preferences.getString(KEY_LOCALE, "en").orEmpty(),
                timezone = preferences.getString(KEY_TIMEZONE, "UTC").orEmpty(),
            ),
            accessToken = accessToken,
            expiresAtEpochMillis = expiresAt,
            refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null)?.let(::decrypt),
        )
    }

    override fun save(session: AuthSession) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, encrypt(session.accessToken))
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochMillis)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken?.let(::encrypt))
            .putString(KEY_USER_KEY, session.user.userKey)
            .putString(KEY_EMAIL, session.user.email)
            .putString(KEY_STATUS, session.user.status)
            .putStringSet(KEY_ROLES, session.user.roles.toSet())
            .putString(KEY_LOCALE, session.user.locale)
            .putString(KEY_TIMEZONE, session.user.timezone)
            .apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_KEY = "user_key"
        const val KEY_EMAIL = "email"
        const val KEY_STATUS = "status"
        const val KEY_ROLES = "roles"
        const val KEY_LOCALE = "locale"
        const val KEY_TIMEZONE = "timezone"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "english_tutor_auth_session"
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload = cipher.iv + encrypted
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String? =
        runCatching {
            val payload = Base64.decode(value, Base64.NO_WRAP)
            val iv = payload.copyOfRange(0, 12)
            val encrypted = payload.copyOfRange(12, payload.size)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrNull()

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return keyGenerator.generateKey()
    }
}

class InMemoryAuthSessionStore(initialSession: AuthSession? = null) : AuthSessionStore {
    private var session: AuthSession? = initialSession

    override fun load(): AuthSession? = session

    override fun save(session: AuthSession) {
        this.session = session
    }

    override fun clear() {
        session = null
    }
}
