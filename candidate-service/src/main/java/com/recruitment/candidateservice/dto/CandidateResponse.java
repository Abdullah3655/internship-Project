package com.recruitment.candidateservice.dto;

import com.recruitment.candidateservice.domain.Candidate;
import com.recruitment.candidateservice.domain.CandidateDocument;
import com.recruitment.candidateservice.domain.CandidateSource;
import com.recruitment.candidateservice.domain.Tag;
import com.recruitment.candidateservice.domain.TalentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CandidateResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        CandidateSource source,
        TalentStatus talentStatus,
        List<String> tags,
        List<DocumentResponse> documents,
        UUID createdByUserId,
        Instant createdAt,
        Instant updatedAt
) {
    public static CandidateResponse from(Candidate candidate) {
        return from(candidate, List.of());
    }

    public static CandidateResponse from(Candidate candidate, List<CandidateDocument> documents) {
        List<String> tags = candidate.getTags().stream().map(Tag::getName).sorted().toList();
        List<DocumentResponse> docs = documents.stream().map(DocumentResponse::from).toList();
        return new CandidateResponse(
                candidate.getId(),
                candidate.getFirstName(),
                candidate.getLastName(),
                candidate.getEmail(),
                candidate.getPhone(),
                candidate.getSource(),
                candidate.getTalentStatus(),
                tags,
                docs,
                candidate.getCreatedByUserId(),
                candidate.getCreatedAt(),
                candidate.getUpdatedAt()
        );
    }
}
