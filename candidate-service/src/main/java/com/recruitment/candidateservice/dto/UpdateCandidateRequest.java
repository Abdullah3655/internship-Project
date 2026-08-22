package com.recruitment.candidateservice.dto;

import com.recruitment.candidateservice.domain.TalentStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateCandidateRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Email String email,
        @Schema(example = "+1234567890")
        @Size(max = 50) String phone,
        @Schema(example = "IN_POOL")
        TalentStatus talentStatus,
        @ArraySchema(schema = @Schema(example = "java"), arraySchema = @Schema(example = "[\"java\", \"senior\"]"))
        List<String> tags
) {
}
