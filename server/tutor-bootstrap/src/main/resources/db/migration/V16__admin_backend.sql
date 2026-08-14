CREATE TABLE system_setting (
    setting_key VARCHAR(128) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    value_type VARCHAR(32) NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at_utc TIMESTAMP(3) NOT NULL,
    updated_at_utc TIMESTAMP(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_system_setting_value_type
        CHECK (value_type IN ('STRING', 'INTEGER', 'BOOLEAN', 'JSON'))
);

CREATE INDEX idx_admin_audit_log_actor_created
    ON admin_audit_log (actor_user_id, created_at_utc);

INSERT INTO system_setting
    (setting_key, setting_value, value_type, description, created_at_utc, updated_at_utc, version)
VALUES
    ('platform.defaultLocale', 'zh-CN', 'STRING', 'Default UI locale', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0),
    ('quota.defaultDailyLimit', '50', 'INTEGER', 'Default daily AI request limit', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0),
    ('maintenance.enabled', 'false', 'BOOLEAN', 'Maintenance mode flag', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0);
