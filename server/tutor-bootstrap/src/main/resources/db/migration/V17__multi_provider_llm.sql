ALTER TABLE ai_provider_config
    DROP CHECK chk_ai_provider_type;

ALTER TABLE ai_provider_config
    ADD CONSTRAINT chk_ai_provider_type
        CHECK (provider_type IN ('OPENAI', 'OPENAI_COMPATIBLE', 'GEMINI'));

INSERT INTO ai_provider_config
    (provider_code, provider_type, display_name, enabled,
     default_llm, default_asr, default_tts, base_url, llm_model,
     created_at_utc, updated_at_utc, version)
VALUES
    ('deepseek', 'OPENAI_COMPATIBLE', 'DeepSeek', TRUE,
     TRUE, FALSE, FALSE, 'https://api.deepseek.com', 'deepseek-v4-flash',
     CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0)
ON DUPLICATE KEY UPDATE
    provider_type = VALUES(provider_type),
    display_name = VALUES(display_name),
    base_url = VALUES(base_url),
    llm_model = COALESCE(ai_provider_config.llm_model, VALUES(llm_model)),
    updated_at_utc = VALUES(updated_at_utc),
    version = ai_provider_config.version + 1;

UPDATE ai_provider_config
SET default_llm = CASE WHEN provider_code = 'deepseek' THEN TRUE ELSE FALSE END,
    updated_at_utc = CURRENT_TIMESTAMP(3),
    version = version + 1
WHERE default_llm = TRUE OR provider_code = 'deepseek';
