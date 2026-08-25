package com.recruitment.authservice.dto;

import com.recruitment.authservice.domain.AccountStatus;
import com.recruitment.authservice.domain.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateManagedUserRequest(
        @NotNull UserRole role,
        @NotNull AccountStatus accountStatus
) {
}
