CREATE TABLE candidates (
    id CHAR(36) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NULL,
    source VARCHAR(32) NOT NULL,
    talent_status VARCHAR(32) NOT NULL,
    created_by_user_id CHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_candidates_email UNIQUE (email)
);

CREATE TABLE tags (
    id CHAR(36) NOT NULL,
    name VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tags_name UNIQUE (name)
);

CREATE TABLE candidate_tags (
    candidate_id CHAR(36) NOT NULL,
    tag_id CHAR(36) NOT NULL,
    PRIMARY KEY (candidate_id, tag_id),
    CONSTRAINT fk_candidate_tags_candidate FOREIGN KEY (candidate_id) REFERENCES candidates (id),
    CONSTRAINT fk_candidate_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id)
);

CREATE TABLE candidate_documents (
    id CHAR(36) NOT NULL,
    candidate_id CHAR(36) NOT NULL,
    document_type VARCHAR(32) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    content_type VARCHAR(128) NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_by_user_id CHAR(36) NOT NULL,
    uploaded_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_candidate_documents_candidate FOREIGN KEY (candidate_id) REFERENCES candidates (id)
);
