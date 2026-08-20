CREATE TABLE app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_key VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE admin_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT,
    action_code VARCHAR(128) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_key VARCHAR(128) NOT NULL,
    before_json VARCHAR(1000),
    after_json VARCHAR(1000),
    created_at_utc TIMESTAMP(3) NOT NULL,
    FOREIGN KEY (actor_user_id) REFERENCES app_user (id)
);

CREATE TABLE user_collection_entitlement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entitlement_key VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    collection_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    granted_by_user_id BIGINT NOT NULL,
    granted_at_utc TIMESTAMP(3) NOT NULL,
    expires_at_utc TIMESTAMP(3),
    revoked_at_utc TIMESTAMP(3),
    reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (user_id, collection_id),
    FOREIGN KEY (user_id) REFERENCES app_user (id),
    FOREIGN KEY (collection_id) REFERENCES resource_collection (id),
    FOREIGN KEY (granted_by_user_id) REFERENCES app_user (id)
);
