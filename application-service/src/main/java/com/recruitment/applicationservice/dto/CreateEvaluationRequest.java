package com.recruitment.applicationservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateEvaluationRequest(
        @Schema(example = "4")
        @NotNull @Min(1) @Max(5) Integer score,
        @Schema(example = "Good interview overall")
        String feedback
) {
}
