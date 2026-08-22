package com.recruitment.applicationservice.dto;

import com.recruitment.applicationservice.domain.ApplicationStageEvent;
import com.recruitment.applicationservice.domain.PipelineStage;

import java.time.Instant;
import java.util.UUID;

public record StageEventResponse(
        UUID id,
        PipelineStage fromStage,
        PipelineStage toStage,
        UUID changedByUserId,
        String note,
        Instant createdAt
) {
    public static StageEventResponse from(ApplicationStageEvent event) {
        return new StageEventResponse(
                event.getId(),
                event.getFromStage(),
                event.getToStage(),
                event.getChangedByUserId(),
                event.getNote(),
                event.getCreatedAt()
        );
    }
}
