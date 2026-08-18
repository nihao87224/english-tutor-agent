package cn.forever24.tutor.infrastructure.provider;

import cn.forever24.tutor.application.provider.ActiveAiProviderConfiguration;
import cn.forever24.tutor.application.provider.AiProviderConfiguration;
import cn.forever24.tutor.application.provider.AiProviderConfigurationDraft;
import cn.forever24.tutor.application.provider.AiProviderConfigurationException;
import cn.forever24.tutor.application.provider.AiProviderConfigurationRepository;
import cn.forever24.tutor.application.provider.AiProviderPurpose;
import cn.forever24.tutor.application.provider.AiProviderType;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class JdbcAiProviderConfigurationRepository implements AiProviderConfigurationRepository {

    private static final String SECRET_TYPE_API_KEY = "API_KEY";

    private final JdbcTemplate jdbcTemplate;
    private final AesGcmSecretCipher secretCipher;
    public JdbcAiProviderConfigurationRepository(
            JdbcTemplate jdbcTemplate,
            AesGcmSecretCipher secretCipher
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.secretCipher = secretCipher;
    }

    @Override
    public List<AiProviderConfiguration> list() {
        return jdbcTemplate.query("""
                        SELECT c.provider_code, c.provider_type, c.display_name, c.enabled,
                               c.default_llm, c.default_asr, c.default_tts, c.base_url,
                               c.llm_model, c.asr_model, c.tts_model, c.tts_voice, c.timeout_ms,
                               s.masked_hint
                        FROM ai_provider_config c
                        LEFT JOIN ai_provider_secret s
                          ON s.provider_code = c.provider_code AND s.secret_type = 'API_KEY'
                        ORDER BY c.provider_code
                        """,
                (rs, rowNum) -> toConfiguration(new ProviderRow(
                        rs.getString("provider_code"),
                        AiProviderType.valueOf(rs.getString("provider_type")),
                        rs.getString("display_name"),
                        rs.getBoolean("enabled"),
                        rs.getBoolean("default_llm"),
                        rs.getBoolean("default_asr"),
                        rs.getBoolean("default_tts"),
                        URI.create(rs.getString("base_url")),
                        rs.getString("llm_model"),
                        rs.getString("asr_model"),
                        rs.getString("tts_model"),
                        rs.getString("tts_voice"),
                        Duration.ofMillis(rs.getLong("timeout_ms")),
                        rs.getString("masked_hint"))));
    }

    @Override
    public Optional<AiProviderConfiguration> findByCode(String providerCode) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                            SELECT c.provider_code, c.provider_type, c.display_name, c.enabled,
                                   c.default_llm, c.default_asr, c.default_tts, c.base_url,
                                   c.llm_model, c.asr_model, c.tts_model, c.tts_voice, c.timeout_ms,
                                   s.masked_hint
                            FROM ai_provider_config c
                            LEFT JOIN ai_provider_secret s
                              ON s.provider_code = c.provider_code AND s.secret_type = 'API_KEY'
                            WHERE c.provider_code = ?
                            """,
                    (rs, rowNum) -> toConfiguration(new ProviderRow(
                            rs.getString("provider_code"),
                            AiProviderType.valueOf(rs.getString("provider_type")),
                            rs.getString("display_name"),
                            rs.getBoolean("enabled"),
                            rs.getBoolean("default_llm"),
                            rs.getBoolean("default_asr"),
                            rs.getBoolean("default_tts"),
                            URI.create(rs.getString("base_url")),
                            rs.getString("llm_model"),
                            rs.getString("asr_model"),
                            rs.getString("tts_model"),
                            rs.getString("tts_voice"),
                            Duration.ofMillis(rs.getLong("timeout_ms")),
                            rs.getString("masked_hint"))),
                    providerCode));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public ActiveAiProviderConfiguration requireDefault(AiProviderPurpose purpose) {
        ProviderRow provider = defaultProvider(purpose);
        String apiKey = decryptApiKey(provider.providerCode());
        return new ActiveAiProviderConfiguration(
                provider.providerCode(),
                provider.providerType(),
                provider.baseUrl(),
                apiKey,
                provider.llmModel(),
                provider.asrModel(),
                provider.ttsModel(),
                provider.ttsVoice(),
                provider.timeout());
    }

    @Override
    public ActiveAiProviderConfiguration requireActive(String providerCode) {
        AiProviderConfiguration provider = findByCode(providerCode)
                .orElseThrow(() -> AiProviderConfigurationException.notFound(providerCode));
        if (!provider.enabled()) {
            throw AiProviderConfigurationException.unavailable("AI provider is disabled: " + providerCode);
        }
        return new ActiveAiProviderConfiguration(
                provider.providerCode(),
                provider.providerType(),
                provider.baseUrl(),
                decryptApiKey(provider.providerCode()),
                provider.llmModel(),
                provider.asrModel(),
                provider.ttsModel(),
                provider.ttsVoice(),
                provider.timeout());
    }

    @Override
    @Transactional
    public AiProviderConfiguration save(AiProviderConfigurationDraft draft, Instant now) {
        jdbcTemplate.update("""
                        INSERT INTO ai_provider_config
                            (provider_code, provider_type, display_name, enabled,
                             default_llm, default_asr, default_tts, base_url,
                             llm_model, asr_model, tts_model, tts_voice, timeout_ms,
                             created_at_utc, updated_at_utc, version)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        ON DUPLICATE KEY UPDATE
                            provider_type = VALUES(provider_type),
                            display_name = VALUES(display_name),
                            enabled = VALUES(enabled),
                            default_llm = VALUES(default_llm),
                            default_asr = VALUES(default_asr),
                            default_tts = VALUES(default_tts),
                            base_url = VALUES(base_url),
                            llm_model = VALUES(llm_model),
                            asr_model = VALUES(asr_model),
                            tts_model = VALUES(tts_model),
                            tts_voice = VALUES(tts_voice),
                            timeout_ms = VALUES(timeout_ms),
                            updated_at_utc = VALUES(updated_at_utc),
                            version = version + 1
                        """,
                draft.providerCode(),
                draft.providerType().name(),
                draft.displayName(),
                draft.enabled(),
                draft.defaultLlm(),
                draft.defaultAsr(),
                draft.defaultTts(),
                draft.baseUrl().toString(),
                draft.llmModel(),
                draft.asrModel(),
                draft.ttsModel(),
                draft.ttsVoice(),
                draft.timeout().toMillis(),
                Timestamp.from(now),
                Timestamp.from(now));

        if (draft.defaultLlm()) {
            unsetOtherDefaults(draft.providerCode(), AiProviderPurpose.LLM, now);
        }
        if (draft.defaultAsr()) {
            unsetOtherDefaults(draft.providerCode(), AiProviderPurpose.ASR, now);
        }
        if (draft.defaultTts()) {
            unsetOtherDefaults(draft.providerCode(), AiProviderPurpose.TTS, now);
        }
        writeAudit(null, "AI_PROVIDER_CONFIG_UPSERTED", "AI_PROVIDER", draft.providerCode(), now);
        return findByCode(draft.providerCode()).orElseThrow();
    }

    @Override
    @Transactional
    public AiProviderConfiguration replaceApiKey(String providerCode, String rawApiKey, long actorUserId, Instant now) {
        if (findByCode(providerCode).isEmpty()) {
            throw AiProviderConfigurationException.notFound(providerCode);
        }
        EncryptedSecret secret = secretCipher.encrypt(rawApiKey);
        String maskedHint = ProviderSecretMask.mask(rawApiKey);
        jdbcTemplate.update("""
                        INSERT INTO ai_provider_secret
                            (provider_code, secret_type, ciphertext, nonce, key_version, masked_hint,
                             created_at_utc, updated_at_utc, version)
                        VALUES (?, 'API_KEY', ?, ?, ?, ?, ?, ?, 0)
                        ON DUPLICATE KEY UPDATE
                            ciphertext = VALUES(ciphertext),
                            nonce = VALUES(nonce),
                            key_version = VALUES(key_version),
                            masked_hint = VALUES(masked_hint),
                            updated_at_utc = VALUES(updated_at_utc),
                            version = version + 1
                        """,
                providerCode,
                secret.ciphertext(),
                secret.nonce(),
                secret.keyVersion(),
                maskedHint,
                Timestamp.from(now),
                Timestamp.from(now));
        writeAudit(actorUserId, "AI_PROVIDER_SECRET_REPLACED", "AI_PROVIDER", providerCode, now);
        return findByCode(providerCode).orElseThrow();
    }

    private ProviderRow defaultProvider(AiProviderPurpose purpose) {
        String defaultColumn = switch (purpose) {
            case LLM -> "default_llm";
            case ASR -> "default_asr";
            case TTS -> "default_tts";
        };
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT provider_code, provider_type, display_name, enabled,
                                   default_llm, default_asr, default_tts, base_url,
                                   llm_model, asr_model, tts_model, tts_voice, timeout_ms
                            FROM ai_provider_config
                            WHERE enabled = TRUE AND %s = TRUE
                            ORDER BY provider_code
                            LIMIT 1
                            """.formatted(defaultColumn),
                    (rs, rowNum) -> new ProviderRow(
                            rs.getString("provider_code"),
                            AiProviderType.valueOf(rs.getString("provider_type")),
                            rs.getString("display_name"),
                            rs.getBoolean("enabled"),
                            rs.getBoolean("default_llm"),
                            rs.getBoolean("default_asr"),
                            rs.getBoolean("default_tts"),
                            URI.create(rs.getString("base_url")),
                            rs.getString("llm_model"),
                            rs.getString("asr_model"),
                            rs.getString("tts_model"),
                            rs.getString("tts_voice"),
                            Duration.ofMillis(rs.getLong("timeout_ms")),
                            null));
        } catch (EmptyResultDataAccessException exception) {
            throw AiProviderConfigurationException.unavailable("No enabled default AI provider for " + purpose.name());
        }
    }

    private String decryptApiKey(String providerCode) {
        try {
            EncryptedSecret secret = jdbcTemplate.queryForObject("""
                            SELECT ciphertext, nonce, key_version
                            FROM ai_provider_secret
                            WHERE provider_code = ? AND secret_type = ?
                            """,
                    (rs, rowNum) -> new EncryptedSecret(
                            rs.getString("ciphertext"),
                            rs.getString("nonce"),
                            rs.getString("key_version")),
                    providerCode,
                    SECRET_TYPE_API_KEY);
            return secretCipher.decrypt(secret);
        } catch (EmptyResultDataAccessException exception) {
            throw AiProviderConfigurationException.unavailable("API key is not configured for provider " + providerCode);
        }
    }

    private void unsetOtherDefaults(String providerCode, AiProviderPurpose purpose, Instant now) {
        String defaultColumn = switch (purpose) {
            case LLM -> "default_llm";
            case ASR -> "default_asr";
            case TTS -> "default_tts";
        };
        jdbcTemplate.update("""
                        UPDATE ai_provider_config
                        SET %s = FALSE,
                            updated_at_utc = ?,
                            version = version + 1
                        WHERE provider_code <> ? AND %s = TRUE
                        """.formatted(defaultColumn, defaultColumn),
                Timestamp.from(now),
                providerCode);
    }

    private void writeAudit(Long actorUserId, String actionCode, String targetType, String targetKey, Instant now) {
        jdbcTemplate.update("""
                        INSERT INTO admin_audit_log
                            (actor_user_id, action_code, target_type, target_key, created_at_utc)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                actorUserId,
                actionCode,
                targetType,
                targetKey,
                Timestamp.from(now));
    }

    private AiProviderConfiguration toConfiguration(ProviderRow row) {
        boolean secretConfigured = row.maskedHint() != null;
        String maskedHint = row.maskedHint();
        return new AiProviderConfiguration(
                row.providerCode(),
                row.providerType(),
                row.displayName(),
                row.enabled(),
                row.defaultLlm(),
                row.defaultAsr(),
                row.defaultTts(),
                row.baseUrl(),
                row.llmModel(),
                row.asrModel(),
                row.ttsModel(),
                row.ttsVoice(),
                row.timeout(),
                secretConfigured,
                maskedHint);
    }

    private record ProviderRow(
            String providerCode,
            AiProviderType providerType,
            String displayName,
            boolean enabled,
            boolean defaultLlm,
            boolean defaultAsr,
            boolean defaultTts,
            URI baseUrl,
            String llmModel,
            String asrModel,
            String ttsModel,
            String ttsVoice,
            Duration timeout,
            String maskedHint
    ) {
    }
}
