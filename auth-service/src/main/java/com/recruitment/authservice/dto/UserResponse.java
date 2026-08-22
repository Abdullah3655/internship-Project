package com.recruitment.authservice.dto;

import com.recruitment.authservice.domain.AccountStatus;
import com.recruitment.authservice.domain.User;
import com.recruitment.authservice.domain.UserRole;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        AccountStatus accountStatus
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getAccountStatus()
        );
    }
}
