package com.recruitment.applicationservice.controller;

import com.recruitment.applicationservice.config.OpenApiConfig;
import com.recruitment.applicationservice.dto.AssignmentListResponse;
import com.recruitment.applicationservice.security.UserPrincipal;
import com.recruitment.applicationservice.service.ApplicationTrackingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assignments")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AssignmentController {

    private final ApplicationTrackingService applicationTrackingService;

    public AssignmentController(ApplicationTrackingService applicationTrackingService) {
        this.applicationTrackingService = applicationTrackingService;
    }

    @GetMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN','INTERVIEWER')")
    public AssignmentListResponse listMine(@AuthenticationPrincipal UserPrincipal actor) {
        return applicationTrackingService.listMyAssignments(actor);
    }
}
