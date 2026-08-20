CREATE TABLE content_provider (
    provider_code VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    CONSTRAINT ck_content_provider_type CHECK (provider_type IN ('INTERNAL', 'THIRD_PARTY'))
);

CREATE TABLE resource_collection (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    collection_key VARCHAR(64) NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    access_scope VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    source_url VARCHAR(1000),
    ownership_type VARCHAR(64) NOT NULL,
    license_note VARCHAR(1000),
    allowed_audience VARCHAR(160) NOT NULL,
    admin_note VARCHAR(1000),
    created_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_resource_collection_key UNIQUE (collection_key),
    CONSTRAINT fk_resource_collection_provider
        FOREIGN KEY (provider_code) REFERENCES content_provider (provider_code),
    CONSTRAINT ck_resource_collection_access_scope
        CHECK (access_scope IN ('PUBLIC', 'ADMIN_GRANTED', 'ADMIN_ONLY', 'DISABLED')),
    CONSTRAINT ck_resource_collection_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX idx_resource_collection_access_status
    ON resource_collection (access_scope, status);

CREATE INDEX idx_resource_collection_provider_status
    ON resource_collection (provider_code, status);

CREATE TABLE learning_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resource_key VARCHAR(180) NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    collection_id BIGINT NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    language VARCHAR(32) NOT NULL,
    level VARCHAR(8) NOT NULL,
    topic VARCHAR(120) NOT NULL,
    scene VARCHAR(120) NOT NULL,
    communication_goal VARCHAR(500) NOT NULL,
    access_scope VARCHAR(32) NOT NULL,
    publish_status VARCHAR(32) NOT NULL,
    active_version_id BIGINT,
    estimated_minutes INT NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_learning_resource_key UNIQUE (resource_key),
    CONSTRAINT fk_learning_resource_provider
        FOREIGN KEY (provider_code) REFERENCES content_provider (provider_code),
    CONSTRAINT fk_learning_resource_collection
        FOREIGN KEY (collection_id) REFERENCES resource_collection (id),
    CONSTRAINT ck_learning_resource_type CHECK (resource_type IN ('SCENARIO_LESSON')),
    CONSTRAINT ck_learning_resource_access_scope
        CHECK (access_scope IN ('PUBLIC', 'ADMIN_GRANTED', 'ADMIN_ONLY', 'DISABLED')),
    CONSTRAINT ck_learning_resource_publish_status
        CHECK (publish_status IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED', 'DISABLED')),
    CONSTRAINT ck_learning_resource_estimated_minutes CHECK (estimated_minutes > 0)
);

CREATE INDEX idx_learning_resource_publish_access_level
    ON learning_resource (publish_status, access_scope, level);

CREATE INDEX idx_learning_resource_topic_scene_level_publish
    ON learning_resource (topic, scene, level, publish_status);

CREATE INDEX idx_learning_resource_collection_publish
    ON learning_resource (collection_id, publish_status);

CREATE TABLE learning_resource_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resource_id BIGINT NOT NULL,
    semantic_version VARCHAR(32) NOT NULL,
    manifest_hash VARCHAR(80) NOT NULL,
    manifest_json JSON NOT NULL,
    learner_fit_json JSON NOT NULL,
    generation_metadata_json JSON NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    published_at_utc DATETIME(3),
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_learning_resource_version UNIQUE (resource_id, semantic_version),
    CONSTRAINT fk_learning_resource_version_resource
        FOREIGN KEY (resource_id) REFERENCES learning_resource (id),
    CONSTRAINT ck_learning_resource_version_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED', 'DISABLED'))
);

ALTER TABLE learning_resource
    ADD CONSTRAINT fk_learning_resource_active_version
    FOREIGN KEY (active_version_id) REFERENCES learning_resource_version (id);

CREATE TABLE learning_asset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    asset_key VARCHAR(200) NOT NULL,
    asset_version VARCHAR(32) NOT NULL,
    media_type VARCHAR(32) NOT NULL,
    purpose VARCHAR(64) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    content_hash VARCHAR(80) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    byte_length BIGINT NOT NULL,
    access_scope VARCHAR(32) NOT NULL,
    metadata_json JSON NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    CONSTRAINT uk_learning_asset_key UNIQUE (asset_key),
    CONSTRAINT uk_learning_asset_object_hash UNIQUE (object_key, content_hash),
    CONSTRAINT ck_learning_asset_media_type CHECK (media_type IN ('IMAGE', 'AUDIO')),
    CONSTRAINT ck_learning_asset_purpose CHECK (purpose IN (
        'TASK_HERO', 'SCENE_STATE', 'CHARACTER_FALLBACK',
        'SCENE_DIALOGUE', 'ROLE_PLAY_PROMPT', 'REVIEW_PROMPT'
    )),
    CONSTRAINT ck_learning_asset_byte_length CHECK (byte_length > 0),
    CONSTRAINT ck_learning_asset_access_scope
        CHECK (access_scope IN ('PUBLIC', 'ADMIN_GRANTED', 'ADMIN_ONLY', 'DISABLED')),
    CONSTRAINT ck_learning_asset_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX idx_learning_asset_purpose_status
    ON learning_asset (purpose, status);

CREATE TABLE resource_version_asset (
    resource_version_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    PRIMARY KEY (resource_version_id, asset_id),
    CONSTRAINT uk_resource_version_asset_order UNIQUE (resource_version_id, display_order),
    CONSTRAINT fk_resource_version_asset_version
        FOREIGN KEY (resource_version_id) REFERENCES learning_resource_version (id) ON DELETE CASCADE,
    CONSTRAINT fk_resource_version_asset_asset
        FOREIGN KEY (asset_id) REFERENCES learning_asset (id),
    CONSTRAINT ck_resource_version_asset_order CHECK (display_order >= 0)
);

CREATE TABLE resource_version_skill_variant (
    resource_version_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    PRIMARY KEY (resource_version_id, variant_id),
    CONSTRAINT fk_resource_version_skill_version
        FOREIGN KEY (resource_version_id) REFERENCES learning_resource_version (id) ON DELETE CASCADE,
    CONSTRAINT fk_resource_version_skill_variant
        FOREIGN KEY (variant_id) REFERENCES curriculum_skill_unit_variant (id)
);

CREATE TABLE collection_resource (
    collection_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (collection_id, resource_id),
    CONSTRAINT fk_collection_resource_collection
        FOREIGN KEY (collection_id) REFERENCES resource_collection (id) ON DELETE CASCADE,
    CONSTRAINT fk_collection_resource_resource
        FOREIGN KEY (resource_id) REFERENCES learning_resource (id) ON DELETE CASCADE,
    CONSTRAINT ck_collection_resource_order CHECK (display_order >= 0),
    CONSTRAINT ck_collection_resource_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);
