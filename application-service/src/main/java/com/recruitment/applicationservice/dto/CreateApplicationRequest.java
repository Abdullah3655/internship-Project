package com.recruitment.applicationservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateApplicationRequest(
        @NotNull UUID jobId,
        @NotNull UUID candidateId
) {
}
