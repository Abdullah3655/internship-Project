package com.recruitment.candidateservice.controller;

import com.recruitment.candidateservice.config.OpenApiConfig;
import com.recruitment.candidateservice.domain.CandidateSource;
import com.recruitment.candidateservice.dto.BulkCvUploadResponse;
import com.recruitment.candidateservice.dto.CandidateListResponse;
import com.recruitment.candidateservice.dto.CandidateResponse;
import com.recruitment.candidateservice.dto.CreateCandidateRequest;
import com.recruitment.candidateservice.dto.CvUploadResponse;
import com.recruitment.candidateservice.dto.UpdateCandidateRequest;
import com.recruitment.candidateservice.security.UserPrincipal;
import com.recruitment.candidateservice.service.CandidateService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/candidates")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @PostMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CandidateResponse create(
            @Valid @RequestBody CreateCandidateRequest request,
            @AuthenticationPrincipal UserPrincipal actor
    ) {
        return candidateService.create(request, actor, CandidateSource.MANUAL);
    }

    @PostMapping(path = "/cv/bulk", version = "1.0", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkCvUploadResponse uploadCvBulk(
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal actor
    ) {
        MultipartFile[] array = files == null ? new MultipartFile[0] : files.toArray(MultipartFile[]::new);
        return candidateService.uploadCvBulk(array, actor);
    }

    @GetMapping(version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN','INTERVIEWER')")
    public CandidateListResponse list(
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String tag
    ) {
        List<String> filter = tags != null && !tags.isEmpty()
                ? tags
                : (tag == null || tag.isBlank() ? List.of() : List.of(tag));
        return candidateService.list(filter);
    }

    @GetMapping(path = "/{id}", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN','INTERVIEWER')")
    public CandidateResponse getById(@PathVariable UUID id) {
        return candidateService.getById(id);
    }

    @PatchMapping(path = "/{id}", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public CandidateResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCandidateRequest request) {
        return candidateService.update(id, request);
    }

    @DeleteMapping(path = "/{id}", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        candidateService.delete(id);
    }

    @PostMapping(path = "/{id}/cv", version = "1.0", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CvUploadResponse uploadCv(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal actor
    ) {
        return candidateService.uploadCv(id, file, actor);
    }

    @GetMapping(path = "/{id}/documents/{documentId}", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN','INTERVIEWER')")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID id,
            @PathVariable UUID documentId
    ) {
        var document = candidateService.loadDocument(id, documentId);
        String contentType = document.contentType() == null || document.contentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : document.contentType();
        String filename = document.originalFilename().replace("\"", "");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(document.resource());
    }

    @DeleteMapping(path = "/{id}/documents/{documentId}", version = "1.0")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable UUID id, @PathVariable UUID documentId) {
        candidateService.deleteDocument(id, documentId);
    }
}
