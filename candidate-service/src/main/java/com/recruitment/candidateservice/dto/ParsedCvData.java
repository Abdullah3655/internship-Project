package com.recruitment.candidateservice.dto;

import java.util.List;

public record ParsedCvData(
        String firstName,
        String lastName,
        String email,
        String phone,
        List<String> tags,
        String rawTextPreview
) {
}
