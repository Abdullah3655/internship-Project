package com.recruitment.applicationservice.dto;

import com.recruitment.applicationservice.domain.EmploymentType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateJobRequest(
        @Schema(example = "Java Engineer")
        @NotBlank @Size(max = 200) String title,
        @Schema(example = "Engineering")
        @Size(max = 100) String department,
        @Schema(example = "Remote")
        @Size(max = 100) String location,
        @Schema(example = "Spring Boot backend role")
        String description,
        @Schema(example = "FULL_TIME")
        @NotNull EmploymentType employmentType,
        @ArraySchema(schema = @Schema(example = "java"), arraySchema = @Schema(example = "[\"java\", \"spring\", \"mysql\"]"))
        List<String> tags
) {
}
