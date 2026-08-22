package com.recruitment.candidateservice.dto;

import com.recruitment.candidateservice.domain.CandidateDocument;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        Instant uploadedAt
) {
    public static DocumentResponse from(CandidateDocument document) {
        return new DocumentResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getUploadedAt()
        );
    }
}
