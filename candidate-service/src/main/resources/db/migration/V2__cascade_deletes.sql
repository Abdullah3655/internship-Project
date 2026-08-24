ALTER TABLE candidate_tags DROP FOREIGN KEY fk_candidate_tags_candidate;
ALTER TABLE candidate_tags ADD CONSTRAINT fk_candidate_tags_candidate
    FOREIGN KEY (candidate_id) REFERENCES candidates (id) ON DELETE CASCADE;

ALTER TABLE candidate_documents DROP FOREIGN KEY fk_candidate_documents_candidate;
ALTER TABLE candidate_documents ADD CONSTRAINT fk_candidate_documents_candidate
    FOREIGN KEY (candidate_id) REFERENCES candidates (id) ON DELETE CASCADE;
