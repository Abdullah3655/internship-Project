package com.recruitment.authservice.dto;

public record AuthResponse(
        String accessToken,
        String tokenType
) {
    public static AuthResponse of(String accessToken) {
        return new AuthResponse(accessToken, "Bearer");
    }
}
