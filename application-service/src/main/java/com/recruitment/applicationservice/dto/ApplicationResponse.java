package com.recruitment.applicationservice.dto;

import com.recruitment.applicationservice.domain.Application;
import com.recruitment.applicationservice.domain.PipelineStage;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID jobId,
        String jobTitle,
        UUID candidateId,
        PipelineStage currentStage,
        Instant createdAt,
        Instant updatedAt
) {
    public static ApplicationResponse from(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getJob().getId(),
                application.getJob().getTitle(),
                application.getCandidateId(),
                application.getCurrentStage(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
