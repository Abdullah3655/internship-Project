package com.recruitment.authservice.controller;

import com.recruitment.authservice.config.OpenApiConfig;
import com.recruitment.authservice.domain.UserRole;
import com.recruitment.authservice.dto.AuthResponse;
import com.recruitment.authservice.dto.LoginRequest;
import com.recruitment.authservice.dto.RefreshRequest;
import com.recruitment.authservice.dto.RegisterRequest;
import com.recruitment.authservice.dto.UpdateManagedUserRequest;
import com.recruitment.authservice.dto.UserListResponse;
import com.recruitment.authservice.dto.UserResponse;
import com.recruitment.authservice.security.UserPrincipal;
import com.recruitment.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/auth")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(path = "/register/hr", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerHr(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request, UserRole.HR);
    }

    @PostMapping(path = "/register/interviewer", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerInterviewer(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request, UserRole.INTERVIEWER);
    }

    @PostMapping(path = "/register/ldap/hr", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerLdapHr(@Valid @RequestBody RegisterRequest request) {
        return authService.registerLdap(request, UserRole.HR);
    }

    @PostMapping(path = "/register/ldap/interviewer", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerLdapInterviewer(@Valid @RequestBody RegisterRequest request) {
        return authService.registerLdap(request, UserRole.INTERVIEWER);
    }

    @PostMapping(path = "/login", version = "1.0")
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = LoginRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "HR",
                                    value = "{\"email\":\"hr@company.com\",\"password\":\"password123\"}"
                            )
                    }
            )
    )
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping(path = "/refresh", version = "1.0")
    @SecurityRequirements
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping(path = "/logout", version = "1.0")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    @GetMapping(path = "/me", version = "1.0")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.toUserResponse(principal.getUser());
    }

    @GetMapping(path = "/users", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public UserListResponse listUsers(@RequestParam(required = false) UserRole role) {
        return authService.listUsers(role);
    }

    @GetMapping(path = "/users/{id}", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public UserResponse getUserById(@PathVariable UUID id) {
        return authService.getById(id);
    }

    @PatchMapping(path = "/users/{id}", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateManagedUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateManagedUserRequest request,
            @AuthenticationPrincipal UserPrincipal actor
    ) {
        return authService.updateManagedUser(id, request, actor);
    }
}
