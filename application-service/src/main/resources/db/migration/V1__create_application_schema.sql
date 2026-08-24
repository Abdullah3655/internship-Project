CREATE TABLE tags (
    id CHAR(36) NOT NULL,
    name VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tags_name UNIQUE (name)
);

CREATE TABLE jobs (
    id CHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    department VARCHAR(100) NULL,
    location VARCHAR(100) NULL,
    description TEXT NULL,
    employment_type VARCHAR(32) NOT NULL,
    job_status VARCHAR(32) NOT NULL,
    created_by_user_id CHAR(36) NOT NULL,
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE job_tags (
    job_id CHAR(36) NOT NULL,
    tag_id CHAR(36) NOT NULL,
    PRIMARY KEY (job_id, tag_id),
    CONSTRAINT fk_job_tags_job FOREIGN KEY (job_id) REFERENCES jobs (id),
    CONSTRAINT fk_job_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id)
);

CREATE TABLE applications (
    id CHAR(36) NOT NULL,
    job_id CHAR(36) NOT NULL,
    candidate_id CHAR(36) NOT NULL,
    current_stage VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_application_job_candidate UNIQUE (job_id, candidate_id),
    CONSTRAINT fk_applications_job FOREIGN KEY (job_id) REFERENCES jobs (id)
);

CREATE TABLE application_assignments (
    id CHAR(36) NOT NULL,
    application_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    assignment_role VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_assignment UNIQUE (application_id, user_id, assignment_role),
    CONSTRAINT fk_assignments_application FOREIGN KEY (application_id) REFERENCES applications (id)
);

CREATE TABLE application_stage_events (
    id CHAR(36) NOT NULL,
    application_id CHAR(36) NOT NULL,
    from_stage VARCHAR(32) NULL,
    to_stage VARCHAR(32) NOT NULL,
    changed_by_user_id CHAR(36) NOT NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_stage_events_application FOREIGN KEY (application_id) REFERENCES applications (id)
);

CREATE TABLE evaluations (
    id CHAR(36) NOT NULL,
    application_id CHAR(36) NOT NULL,
    interviewer_user_id CHAR(36) NOT NULL,
    score INT NOT NULL,
    feedback TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_evaluations_application FOREIGN KEY (application_id) REFERENCES applications (id)
);
