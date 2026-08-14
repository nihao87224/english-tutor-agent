CREATE TABLE ai_provider_config (
    provider_code VARCHAR(64) PRIMARY KEY,
    provider_type VARCHAR(32) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    default_llm BOOLEAN NOT NULL DEFAULT FALSE,
    default_asr BOOLEAN NOT NULL DEFAULT FALSE,
    default_tts BOOLEAN NOT NULL DEFAULT FALSE,
    base_url VARCHAR(512) NOT NULL,
    llm_model VARCHAR(128) NULL,
    asr_model VARCHAR(128) NULL,
    tts_model VARCHAR(128) NULL,
    tts_voice VARCHAR(128) NULL,
    timeout_ms BIGINT NOT NULL DEFAULT 30000,
    created_at_utc TIMESTAMP(3) NOT NULL,
    updated_at_utc TIMESTAMP(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_ai_provider_type CHECK (provider_type IN ('OPENAI'))
);

CREATE TABLE ai_provider_secret (
    provider_code VARCHAR(64) NOT NULL,
    secret_type VARCHAR(32) NOT NULL,
    ciphertext TEXT NOT NULL,
    nonce VARCHAR(64) NOT NULL,
    key_version VARCHAR(32) NOT NULL,
    masked_hint VARCHAR(32) NOT NULL,
    created_at_utc TIMESTAMP(3) NOT NULL,
    updated_at_utc TIMESTAMP(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (provider_code, secret_type),
    CONSTRAINT fk_ai_provider_secret_provider
        FOREIGN KEY (provider_code) REFERENCES ai_provider_config(provider_code)
);

CREATE TABLE admin_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_user_id BIGINT NULL,
    action_code VARCHAR(128) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_key VARCHAR(128) NOT NULL,
    before_json JSON NULL,
    after_json JSON NULL,
    request_id VARCHAR(128) NULL,
    request_ip VARCHAR(64) NULL,
    user_agent VARCHAR(512) NULL,
    created_at_utc TIMESTAMP(3) NOT NULL,
    CONSTRAINT fk_admin_audit_actor
        FOREIGN KEY (actor_user_id) REFERENCES app_user(id)
);

CREATE INDEX idx_ai_provider_config_default_llm
    ON ai_provider_config (enabled, default_llm);

CREATE INDEX idx_ai_provider_config_default_asr
    ON ai_provider_config (enabled, default_asr);

CREATE INDEX idx_ai_provider_config_default_tts
    ON ai_provider_config (enabled, default_tts);

CREATE INDEX idx_admin_audit_log_created
    ON admin_audit_log (created_at_utc);

INSERT INTO ai_provider_config
    (provider_code, provider_type, display_name, enabled,
     default_llm, default_asr, default_tts, base_url,
     created_at_utc, updated_at_utc, version)
VALUES
    ('openai', 'OPENAI', 'OpenAI', TRUE,
     TRUE, TRUE, TRUE, 'https://api.openai.com/v1',
     CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0);
