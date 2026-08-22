package com.recruitment.candidateservice.dto;

public record BulkCvUploadItemResponse(
        String filename,
        boolean success,
        CandidateResponse candidate,
        DocumentResponse document,
        ParsedCvData parsed,
        String error
) {
    public static BulkCvUploadItemResponse ok(
            String filename,
            CandidateResponse candidate,
            DocumentResponse document,
            ParsedCvData parsed
    ) {
        return new BulkCvUploadItemResponse(filename, true, candidate, document, parsed, null);
    }

    public static BulkCvUploadItemResponse fail(String filename, String error) {
        return new BulkCvUploadItemResponse(filename, false, null, null, null, error);
    }
}
