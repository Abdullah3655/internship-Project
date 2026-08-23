package com.recruitment.applicationservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthUserView(
        UUID id,
        String email,
        String role,
        String accountStatus
) {
}
