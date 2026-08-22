package com.recruitment.candidateservice.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateCandidateRequest(
        @Schema(example = "Alice")
        @NotBlank @Size(max = 100) String firstName,
        @Schema(example = "Smith")
        @NotBlank @Size(max = 100) String lastName,
        @Schema(example = "alice@example.com")
        @NotBlank @Email String email,
        @Schema(example = "+1234567890")
        @Size(max = 50) String phone,
        @ArraySchema(schema = @Schema(example = "java"), arraySchema = @Schema(example = "[\"java\", \"spring\"]"))
        List<String> tags
) {
}
