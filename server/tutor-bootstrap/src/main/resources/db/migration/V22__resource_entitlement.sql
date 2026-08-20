CREATE TABLE user_collection_entitlement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entitlement_key VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    collection_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    granted_by_user_id BIGINT NOT NULL,
    granted_at_utc DATETIME(3) NOT NULL,
    expires_at_utc DATETIME(3),
    revoked_at_utc DATETIME(3),
    reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_collection_entitlement_key UNIQUE (entitlement_key),
    CONSTRAINT uk_user_collection_entitlement_user_collection UNIQUE (user_id, collection_id),
    CONSTRAINT fk_user_collection_entitlement_user
        FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_user_collection_entitlement_collection
        FOREIGN KEY (collection_id) REFERENCES resource_collection (id),
    CONSTRAINT fk_user_collection_entitlement_granted_by
        FOREIGN KEY (granted_by_user_id) REFERENCES app_user (id),
    CONSTRAINT ck_user_collection_entitlement_status
        CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED'))
);

CREATE INDEX idx_user_collection_entitlement_user_status_expiry
    ON user_collection_entitlement (user_id, status, expires_at_utc);

CREATE INDEX idx_user_collection_entitlement_collection_status
    ON user_collection_entitlement (collection_id, status);

INSERT INTO app_permission (code, description, created_at_utc)
VALUES
    ('RESOURCE_READ', 'Read learning resource metadata', CURRENT_TIMESTAMP(3)),
    ('RESOURCE_MANAGE', 'Manage learning resource metadata', CURRENT_TIMESTAMP(3)),
    ('RESOURCE_PUBLISH', 'Publish learning resource versions', CURRENT_TIMESTAMP(3)),
    ('COLLECTION_READ', 'Read resource collections', CURRENT_TIMESTAMP(3)),
    ('COLLECTION_MANAGE', 'Manage resource collections', CURRENT_TIMESTAMP(3)),
    ('ENTITLEMENT_READ', 'Read resource entitlements', CURRENT_TIMESTAMP(3)),
    ('ENTITLEMENT_MANAGE', 'Grant, revoke, and expire resource entitlements', CURRENT_TIMESTAMP(3));

INSERT INTO app_role_permission (role_id, permission_id, created_at_utc)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM app_role r
JOIN app_permission p
    ON p.code IN (
        'RESOURCE_READ', 'RESOURCE_MANAGE', 'RESOURCE_PUBLISH',
        'COLLECTION_READ', 'COLLECTION_MANAGE',
        'ENTITLEMENT_READ', 'ENTITLEMENT_MANAGE'
    )
WHERE r.code = 'ADMIN';
