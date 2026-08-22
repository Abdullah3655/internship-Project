package com.recruitment.applicationservice.dto;

import com.recruitment.applicationservice.domain.Evaluation;

import java.time.Instant;
import java.util.UUID;

public record EvaluationResponse(
        UUID id,
        UUID interviewerUserId,
        int score,
        String feedback,
        Instant createdAt
) {
    public static EvaluationResponse from(Evaluation evaluation) {
        return new EvaluationResponse(
                evaluation.getId(),
                evaluation.getInterviewerUserId(),
                evaluation.getScore(),
                evaluation.getFeedback(),
                evaluation.getCreatedAt()
        );
    }
}
