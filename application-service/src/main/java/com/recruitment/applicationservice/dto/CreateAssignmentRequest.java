package com.recruitment.applicationservice.dto;

import com.recruitment.applicationservice.domain.AssignmentRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAssignmentRequest(
        @NotNull UUID userId,
        @Schema(example = "INTERVIEWER")
        @NotNull AssignmentRole assignmentRole
) {
}
