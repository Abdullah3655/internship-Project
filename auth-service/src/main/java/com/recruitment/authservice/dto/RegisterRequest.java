package com.recruitment.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "new.hr@company.com")
        @NotBlank @Email String email,
        @Schema(example = "password123")
        @NotBlank @Size(min = 8, max = 72) String password,
        @Schema(example = "Nina")
        @NotBlank @Size(max = 100) String firstName,
        @Schema(example = "Recruiter")
        @NotBlank @Size(max = 100) String lastName
) {
}
