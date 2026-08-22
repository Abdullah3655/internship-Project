package com.recruitment.applicationservice.controller;

import com.recruitment.applicationservice.config.OpenApiConfig;
import com.recruitment.applicationservice.domain.PipelineStage;
import com.recruitment.applicationservice.dto.ApplicationListResponse;
import com.recruitment.applicationservice.dto.ApplicationResponse;
import com.recruitment.applicationservice.dto.AssignmentListResponse;
import com.recruitment.applicationservice.dto.AssignmentResponse;
import com.recruitment.applicationservice.dto.CreateApplicationRequest;
import com.recruitment.applicationservice.dto.CreateAssignmentRequest;
import com.recruitment.applicationservice.dto.CreateEvaluationRequest;
import com.recruitment.applicationservice.dto.EvaluationListResponse;
import com.recruitment.applicationservice.dto.EvaluationResponse;
import com.recruitment.applicationservice.dto.StageChangeRequest;
import com.recruitment.applicationservice.dto.StageEventListResponse;
import com.recruitment.applicationservice.dto.StageEventResponse;
import com.recruitment.applicationservice.security.UserPrincipal;
import com.recruitment.applicationservice.service.ApplicationTrackingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ApplicationController {

    private final ApplicationTrackingService applicationTrackingService;

    public ApplicationController(ApplicationTrackingService applicationTrackingService) {
        this.applicationTrackingService = applicationTrackingService;
    }

    @PostMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(
            @Valid @RequestBody CreateApplicationRequest request,
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return applicationTrackingService.create(request, actor, authorization);
    }

    @GetMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN','INTERVIEWER')")
    public ApplicationListResponse list(
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) UUID candidateId,
            @RequestParam(required = false) PipelineStage stage
    ) {
        return applicationTrackingService.list(jobId, candidateId, stage);
    }

    @GetMapping(path = "/{id}", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN','INTERVIEWER')")
    public ApplicationResponse getById(@PathVariable UUID id) {
        return applicationTrackingService.getById(id);
    }

    @DeleteMapping(path = "/{id}", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        applicationTrackingService.delete(id);
    }

    @PostMapping(path = "/{id}/stage-changes", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public StageEventResponse changeStage(
            @PathVariable UUID id,
            @Valid @RequestBody StageChangeRequest request,
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return applicationTrackingService.changeStage(id, request, actor, authorization);
    }

    @GetMapping(path = "/{id}/stage-changes", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN','INTERVIEWER')")
    public StageEventListResponse listStageChanges(@PathVariable UUID id) {
        return applicationTrackingService.listStageChanges(id);
    }

    @PostMapping(path = "/{id}/assignments", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentResponse assign(
            @PathVariable UUID id,
            @Valid @RequestBody CreateAssignmentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return applicationTrackingService.assign(id, request, authorization);
    }

    @DeleteMapping(path = "/{id}/assignments/{assignmentId}", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(@PathVariable UUID id, @PathVariable UUID assignmentId) {
        applicationTrackingService.deleteAssignment(id, assignmentId);
    }

    @GetMapping(path = "/{id}/assignments", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN','INTERVIEWER')")
    public AssignmentListResponse listAssignments(@PathVariable UUID id) {
        return applicationTrackingService.listAssignments(id);
    }

    @PostMapping(path = "/{id}/evaluations", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN','INTERVIEWER')")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationResponse evaluate(
            @PathVariable UUID id,
            @Valid @RequestBody CreateEvaluationRequest request,
            @AuthenticationPrincipal UserPrincipal actor
    ) {
        return applicationTrackingService.evaluate(id, request, actor);
    }

    @DeleteMapping(path = "/{id}/evaluations/{evaluationId}", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvaluation(@PathVariable UUID id, @PathVariable UUID evaluationId) {
        applicationTrackingService.deleteEvaluation(id, evaluationId);
    }

    @GetMapping(path = "/{id}/evaluations", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN','INTERVIEWER')")
    public EvaluationListResponse listEvaluations(@PathVariable UUID id) {
        return applicationTrackingService.listEvaluations(id);
    }
}
