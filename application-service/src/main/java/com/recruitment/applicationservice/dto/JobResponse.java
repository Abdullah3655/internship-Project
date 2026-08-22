package com.recruitment.applicationservice.dto;

import com.recruitment.applicationservice.domain.EmploymentType;
import com.recruitment.applicationservice.domain.Job;
import com.recruitment.applicationservice.domain.JobStatus;
import com.recruitment.applicationservice.domain.Tag;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String title,
        String department,
        String location,
        String description,
        EmploymentType employmentType,
        JobStatus jobStatus,
        List<String> tags,
        UUID createdByUserId,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static JobResponse from(Job job) {
        List<String> tags = job.getTags().stream().map(Tag::getName).sorted().toList();
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getDepartment(),
                job.getLocation(),
                job.getDescription(),
                job.getEmploymentType(),
                job.getJobStatus(),
                tags,
                job.getCreatedByUserId(),
                job.getPublishedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
