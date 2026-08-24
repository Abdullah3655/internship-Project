ALTER TABLE job_tags DROP FOREIGN KEY fk_job_tags_job;
ALTER TABLE job_tags ADD CONSTRAINT fk_job_tags_job
    FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE;

ALTER TABLE applications DROP FOREIGN KEY fk_applications_job;
ALTER TABLE applications ADD CONSTRAINT fk_applications_job
    FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE;

ALTER TABLE application_assignments DROP FOREIGN KEY fk_assignments_application;
ALTER TABLE application_assignments ADD CONSTRAINT fk_assignments_application
    FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE;

ALTER TABLE application_stage_events DROP FOREIGN KEY fk_stage_events_application;
ALTER TABLE application_stage_events ADD CONSTRAINT fk_stage_events_application
    FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE;

ALTER TABLE evaluations DROP FOREIGN KEY fk_evaluations_application;
ALTER TABLE evaluations ADD CONSTRAINT fk_evaluations_application
    FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE;
