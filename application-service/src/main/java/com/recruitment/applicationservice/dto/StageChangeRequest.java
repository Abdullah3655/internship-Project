package com.recruitment.applicationservice.dto;

import com.recruitment.applicationservice.domain.PipelineStage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StageChangeRequest(
        @Schema(example = "INTERVIEW")
        @NotNull PipelineStage toStage,
        @Schema(example = "Passed screening")
        @Size(max = 1000) String note
) {
}
