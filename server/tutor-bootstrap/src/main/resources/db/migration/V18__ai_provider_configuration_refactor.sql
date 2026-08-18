-- V15-V17 introduced provider tables and seeded defaults before a database secret was required.
-- A provider without an encrypted API key is not runtime-ready, so it must not remain default.
UPDATE ai_provider_config c
LEFT JOIN ai_provider_secret s
  ON s.provider_code = c.provider_code
 AND s.secret_type = 'API_KEY'
SET c.default_llm = FALSE,
    c.default_asr = FALSE,
    c.default_tts = FALSE,
    c.updated_at_utc = CURRENT_TIMESTAMP(3),
    c.version = c.version + 1
WHERE s.provider_code IS NULL
  AND (c.default_llm = TRUE OR c.default_asr = TRUE OR c.default_tts = TRUE);
