package com.recruitment.candidateservice.dto;

import java.util.List;

public record BulkCvUploadResponse(List<BulkCvUploadItemResponse> items) {
}
