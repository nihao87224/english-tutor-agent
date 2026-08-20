CREATE TABLE content_provider (
    provider_code VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL,
    provider_type VARCHAR(32) NOT NULL
);

CREATE TABLE resource_collection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    collection_key VARCHAR(64) NOT NULL UNIQUE,
    provider_code VARCHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    access_scope VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    source_url VARCHAR(1000),
    ownership_type VARCHAR(64) NOT NULL,
    license_note VARCHAR(1000),
    allowed_audience VARCHAR(160) NOT NULL,
    admin_note VARCHAR(1000),
    created_at_utc TIMESTAMP(3) NOT NULL,
    updated_at_utc TIMESTAMP(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (provider_code) REFERENCES content_provider (provider_code)
);

CREATE TABLE learning_resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_key VARCHAR(180) NOT NULL UNIQUE,
    provider_code VARCHAR(64) NOT NULL,
    collection_id BIGINT NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description CLOB,
    language VARCHAR(32) NOT NULL,
    level VARCHAR(8) NOT NULL,
    topic VARCHAR(120) NOT NULL,
    scene VARCHAR(120) NOT NULL,
    communication_goal VARCHAR(500) NOT NULL,
    access_scope VARCHAR(32) NOT NULL,
    publish_status VARCHAR(32) NOT NULL,
    active_version_id BIGINT,
    estimated_minutes INT NOT NULL,
    created_at_utc TIMESTAMP(3) NOT NULL,
    updated_at_utc TIMESTAMP(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (provider_code) REFERENCES content_provider (provider_code),
    FOREIGN KEY (collection_id) REFERENCES resource_collection (id)
);

CREATE TABLE learning_resource_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    semantic_version VARCHAR(32) NOT NULL,
    manifest_hash VARCHAR(80) NOT NULL,
    manifest_json VARCHAR(12000) NOT NULL,
    learner_fit_json VARCHAR(4000) NOT NULL,
    generation_metadata_json VARCHAR(4000) NOT NULL,
    created_at_utc TIMESTAMP(3) NOT NULL,
    published_at_utc TIMESTAMP(3),
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (resource_id, semantic_version),
    FOREIGN KEY (resource_id) REFERENCES learning_resource (id)
);

ALTER TABLE learning_resource
    ADD FOREIGN KEY (active_version_id) REFERENCES learning_resource_version (id);

CREATE TABLE learning_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_key VARCHAR(200) NOT NULL UNIQUE,
    asset_version VARCHAR(32) NOT NULL,
    media_type VARCHAR(32) NOT NULL,
    purpose VARCHAR(64) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    content_hash VARCHAR(80) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    byte_length BIGINT NOT NULL,
    access_scope VARCHAR(32) NOT NULL,
    metadata_json VARCHAR(12000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at_utc TIMESTAMP(3) NOT NULL,
    UNIQUE (object_key, content_hash)
);

CREATE TABLE resource_version_asset (
    resource_version_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    PRIMARY KEY (resource_version_id, asset_id),
    UNIQUE (resource_version_id, display_order),
    FOREIGN KEY (resource_version_id) REFERENCES learning_resource_version (id) ON DELETE CASCADE,
    FOREIGN KEY (asset_id) REFERENCES learning_asset (id)
);

CREATE TABLE resource_version_skill_variant (
    resource_version_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    PRIMARY KEY (resource_version_id, variant_id),
    FOREIGN KEY (resource_version_id) REFERENCES learning_resource_version (id) ON DELETE CASCADE,
    FOREIGN KEY (variant_id) REFERENCES curriculum_skill_unit_variant (id)
);

CREATE TABLE collection_resource (
    collection_id BIGINT NOT NULL,
    resource_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (collection_id, resource_id),
    FOREIGN KEY (collection_id) REFERENCES resource_collection (id) ON DELETE CASCADE,
    FOREIGN KEY (resource_id) REFERENCES learning_resource (id) ON DELETE CASCADE
);
