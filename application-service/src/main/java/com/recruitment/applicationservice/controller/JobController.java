package com.recruitment.applicationservice.controller;

import com.recruitment.applicationservice.config.OpenApiConfig;
import com.recruitment.applicationservice.domain.JobStatus;
import com.recruitment.applicationservice.dto.CreateJobRequest;
import com.recruitment.applicationservice.dto.JobListResponse;
import com.recruitment.applicationservice.dto.JobResponse;
import com.recruitment.applicationservice.dto.UpdateJobRequest;
import com.recruitment.applicationservice.security.UserPrincipal;
import com.recruitment.applicationservice.service.JobService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse create(
            @Valid @RequestBody CreateJobRequest request,
            @AuthenticationPrincipal UserPrincipal actor
    ) {
        return jobService.create(request, actor);
    }

    @GetMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN','INTERVIEWER')")
    public JobListResponse list(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String tag
    ) {
        return jobService.list(status, tag);
    }

    @GetMapping(path = "/{id}", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN','INTERVIEWER')")
    public JobResponse getById(@PathVariable UUID id) {
        return jobService.getById(id);
    }

    @PatchMapping(path = "/{id}", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public JobResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateJobRequest request) {
        return jobService.update(id, request);
    }

    @PostMapping(path = "/{id}/publish", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public JobResponse publish(@PathVariable UUID id) {
        return jobService.publish(id);
    }

    @DeleteMapping(path = "/{id}", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        jobService.delete(id);
    }
}
