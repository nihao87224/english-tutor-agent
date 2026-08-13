CREATE TABLE app_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_key VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    locale VARCHAR(16) NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uk_app_user_user_key UNIQUE (user_key)
);

CREATE TABLE user_learning_profile (
    user_id BIGINT NOT NULL PRIMARY KEY,
    primary_goal VARCHAR(32) NULL,
    daily_minutes INT NOT NULL DEFAULT 20,
    correction_preference VARCHAR(32) NOT NULL DEFAULT 'STANDARD',
    onboarding_status VARCHAR(32) NOT NULL,
    profile_version BIGINT NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_user_learning_profile_user
        FOREIGN KEY (user_id) REFERENCES app_user (id)
);
