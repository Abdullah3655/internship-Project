package com.recruitment.applicationservice.dto;

import com.recruitment.applicationservice.domain.EmploymentType;
import com.recruitment.applicationservice.domain.JobStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateJobRequest(
        @Size(max = 200) String title,
        @Size(max = 100) String department,
        @Schema(example = "Amman")
        @Size(max = 100) String location,
        @Schema(example = "Updated job description")
        String description,
        EmploymentType employmentType,
        JobStatus jobStatus,
        @ArraySchema(schema = @Schema(example = "java"), arraySchema = @Schema(example = "[\"java\", \"spring\", \"kafka\"]"))
        List<String> tags
) {
}
