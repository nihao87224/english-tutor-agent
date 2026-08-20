CREATE TABLE experience_season (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    season_key VARCHAR(8) NOT NULL,
    title VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    metadata_json JSON NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_experience_season_key UNIQUE (season_key),
    CONSTRAINT ck_experience_season_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE experience_episode (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    episode_key VARCHAR(8) NOT NULL,
    season_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    story_anchor VARCHAR(1000) NOT NULL,
    story_order_required BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL,
    metadata_json JSON NOT NULL,
    sequence_no INT NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_experience_episode_key UNIQUE (episode_key),
    CONSTRAINT fk_experience_episode_season
        FOREIGN KEY (season_id) REFERENCES experience_season (id) ON DELETE CASCADE,
    CONSTRAINT ck_experience_episode_story_order CHECK (story_order_required = FALSE),
    CONSTRAINT ck_experience_episode_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_experience_episode_sequence CHECK (sequence_no >= 0)
);

CREATE INDEX idx_experience_episode_season_status
    ON experience_episode (season_id, status);

CREATE TABLE experience_scene (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scene_key VARCHAR(32) NOT NULL,
    episode_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    location VARCHAR(240) NOT NULL,
    story_context VARCHAR(1000) NOT NULL,
    character_state_json JSON NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_experience_scene_key UNIQUE (scene_key),
    CONSTRAINT fk_experience_scene_episode
        FOREIGN KEY (episode_id) REFERENCES experience_episode (id) ON DELETE CASCADE,
    CONSTRAINT ck_experience_scene_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX idx_experience_scene_episode_status
    ON experience_scene (episode_id, status);

CREATE TABLE episode_mapping (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mapping_key VARCHAR(180) NOT NULL,
    variant_id BIGINT NOT NULL,
    episode_id BIGINT NOT NULL,
    scene_id BIGINT NOT NULL,
    eligible_levels_json JSON NOT NULL,
    learner_fit_json JSON NOT NULL,
    story_transition_json JSON NOT NULL,
    fit_inputs_json JSON NOT NULL,
    fallback_mapping_id BIGINT,
    status VARCHAR(32) NOT NULL,
    created_at_utc DATETIME(3) NOT NULL,
    updated_at_utc DATETIME(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_episode_mapping_key UNIQUE (mapping_key),
    CONSTRAINT fk_episode_mapping_variant
        FOREIGN KEY (variant_id) REFERENCES curriculum_skill_unit_variant (id),
    CONSTRAINT fk_episode_mapping_episode
        FOREIGN KEY (episode_id) REFERENCES experience_episode (id) ON DELETE CASCADE,
    CONSTRAINT fk_episode_mapping_scene
        FOREIGN KEY (scene_id) REFERENCES experience_scene (id) ON DELETE CASCADE,
    CONSTRAINT fk_episode_mapping_fallback
        FOREIGN KEY (fallback_mapping_id) REFERENCES episode_mapping (id) ON DELETE SET NULL,
    CONSTRAINT ck_episode_mapping_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX idx_episode_mapping_variant_status
    ON episode_mapping (variant_id, status);

CREATE INDEX idx_episode_mapping_episode_scene_status
    ON episode_mapping (episode_id, scene_id, status);

CREATE TABLE episode_mapping_resource (
    mapping_id BIGINT NOT NULL,
    resource_version_id BIGINT NOT NULL,
    priority INT NOT NULL,
    PRIMARY KEY (mapping_id, resource_version_id),
    CONSTRAINT uk_episode_mapping_resource_priority UNIQUE (mapping_id, priority),
    CONSTRAINT fk_episode_mapping_resource_mapping
        FOREIGN KEY (mapping_id) REFERENCES episode_mapping (id) ON DELETE CASCADE,
    CONSTRAINT fk_episode_mapping_resource_version
        FOREIGN KEY (resource_version_id) REFERENCES learning_resource_version (id),
    CONSTRAINT ck_episode_mapping_resource_priority CHECK (priority >= 0)
);
