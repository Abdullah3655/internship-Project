package com.recruitment.candidateservice.dto;

public record CvUploadResponse(
        DocumentResponse document,
        CandidateResponse candidate,
        ParsedCvData parsed
) {
}
