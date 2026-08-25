package com.recruitment.candidateservice.repository;

import com.recruitment.candidateservice.domain.CandidateDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateDocumentRepository extends JpaRepository<CandidateDocument, UUID> {

    List<CandidateDocument> findByCandidateIdOrderByUploadedAtDesc(UUID candidateId);

    List<CandidateDocument> findByCandidateIdInOrderByUploadedAtDesc(Collection<UUID> candidateIds);

    Optional<CandidateDocument> findByIdAndCandidateId(UUID id, UUID candidateId);
}
