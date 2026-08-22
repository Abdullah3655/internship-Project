package com.recruitment.candidateservice.repository;

import com.recruitment.candidateservice.domain.CandidateDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateDocumentRepository extends JpaRepository<CandidateDocument, UUID> {

    List<CandidateDocument> findByCandidateIdOrderByUploadedAtDesc(UUID candidateId);
}
