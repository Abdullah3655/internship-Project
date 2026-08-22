package com.recruitment.applicationservice.dto;

import com.recruitment.applicationservice.domain.ApplicationAssignment;
import com.recruitment.applicationservice.domain.AssignmentRole;

import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID applicationId,
        UUID userId,
        AssignmentRole assignmentRole,
        Instant createdAt
) {
    public static AssignmentResponse from(ApplicationAssignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getApplication().getId(),
                assignment.getUserId(),
                assignment.getAssignmentRole(),
                assignment.getCreatedAt()
        );
    }
}
