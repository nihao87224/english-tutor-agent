CREATE TABLE content_import_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_key VARCHAR(96) NOT NULL,
    content_hash VARCHAR(80) NOT NULL,
    status VARCHAR(32) NOT NULL,
    resource_key VARCHAR(180) NULL,
    created_at_utc DATETIME(3) NOT NULL,
    completed_at_utc DATETIME(3) NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uq_content_import_batch_key UNIQUE (batch_key),
    CONSTRAINT uq_content_import_batch_hash UNIQUE (content_hash),
    CONSTRAINT ck_content_import_batch_status CHECK (status IN ('RECEIVED', 'REJECTED', 'IMPORTED_DRAFT', 'IMPORT_FAILED'))
);

CREATE TABLE content_import_issue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    issue_code VARCHAR(64) NOT NULL,
    location VARCHAR(320) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    sequence_no INT NOT NULL,
    CONSTRAINT fk_content_import_issue_batch FOREIGN KEY (batch_id) REFERENCES content_import_batch (id),
    CONSTRAINT uq_content_import_issue_sequence UNIQUE (batch_id, sequence_no)
);
