CREATE TABLE app_role (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    CONSTRAINT uk_app_role_code UNIQUE (code)
);

CREATE TABLE app_permission (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(128) NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    CONSTRAINT uk_app_permission_code UNIQUE (code)
);

CREATE TABLE app_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role
        FOREIGN KEY (role_id) REFERENCES app_role (id),
    CONSTRAINT fk_role_permission_permission
        FOREIGN KEY (permission_id) REFERENCES app_permission (id)
);

CREATE TABLE app_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user
        FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_user_role_role
        FOREIGN KEY (role_id) REFERENCES app_role (id)
);

INSERT INTO app_role (code, name, created_at_utc)
VALUES
    ('USER', 'Learner', CURRENT_TIMESTAMP(3)),
    ('ADMIN', 'Administrator', CURRENT_TIMESTAMP(3));

INSERT INTO app_permission (code, description, created_at_utc)
VALUES
    ('DASHBOARD_READ', 'Read admin dashboard', CURRENT_TIMESTAMP(3)),
    ('USER_READ', 'Read users', CURRENT_TIMESTAMP(3)),
    ('USER_UPDATE', 'Update users', CURRENT_TIMESTAMP(3)),
    ('USER_STATUS_MANAGE', 'Enable or disable users', CURRENT_TIMESTAMP(3)),
    ('USER_QUOTA_MANAGE', 'Manage user quota', CURRENT_TIMESTAMP(3)),
    ('USER_ROLE_MANAGE', 'Manage user roles', CURRENT_TIMESTAMP(3)),
    ('AI_PROVIDER_READ', 'Read AI provider configuration', CURRENT_TIMESTAMP(3)),
    ('AI_PROVIDER_MANAGE', 'Manage AI provider configuration', CURRENT_TIMESTAMP(3)),
    ('SYSTEM_SETTING_READ', 'Read system settings', CURRENT_TIMESTAMP(3)),
    ('SYSTEM_SETTING_MANAGE', 'Manage system settings', CURRENT_TIMESTAMP(3)),
    ('AUDIT_READ', 'Read audit log', CURRENT_TIMESTAMP(3));

INSERT INTO app_role_permission (role_id, permission_id, created_at_utc)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM app_role r
JOIN app_permission p
WHERE r.code = 'ADMIN';
