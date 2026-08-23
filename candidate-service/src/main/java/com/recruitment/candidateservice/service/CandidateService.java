package com.recruitment.candidateservice.service;

import com.recruitment.candidateservice.domain.Candidate;
import com.recruitment.candidateservice.domain.CandidateDocument;
import com.recruitment.candidateservice.domain.CandidateSource;
import com.recruitment.candidateservice.domain.DocumentType;
import com.recruitment.candidateservice.domain.Tag;
import com.recruitment.candidateservice.domain.TalentStatus;
import com.recruitment.candidateservice.dto.BulkCvUploadItemResponse;
import com.recruitment.candidateservice.dto.BulkCvUploadResponse;
import com.recruitment.candidateservice.dto.CandidateListResponse;
import com.recruitment.candidateservice.dto.CandidateResponse;
import com.recruitment.candidateservice.dto.CreateCandidateRequest;
import com.recruitment.candidateservice.dto.CvUploadResponse;
import com.recruitment.candidateservice.dto.DocumentResponse;
import com.recruitment.candidateservice.dto.ParsedCvData;
import com.recruitment.candidateservice.dto.UpdateCandidateRequest;
import com.recruitment.candidateservice.exception.CandidateNotFoundException;
import com.recruitment.candidateservice.exception.DuplicateCandidateEmailException;
import com.recruitment.candidateservice.exception.InvalidFileException;
import com.recruitment.candidateservice.repository.CandidateDocumentRepository;
import com.recruitment.candidateservice.repository.CandidateRepository;
import com.recruitment.candidateservice.repository.TagRepository;
import com.recruitment.candidateservice.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final TagRepository tagRepository;
    private final CandidateDocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final CvParseService cvParseService;
    private final TransactionTemplate transactionTemplate;

    public CandidateService(
            CandidateRepository candidateRepository,
            TagRepository tagRepository,
            CandidateDocumentRepository documentRepository,
            FileStorageService fileStorageService,
            CvParseService cvParseService,
            PlatformTransactionManager transactionManager
    ) {
        this.candidateRepository = candidateRepository;
        this.tagRepository = tagRepository;
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.cvParseService = cvParseService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public CandidateResponse create(CreateCandidateRequest request, UserPrincipal actor, CandidateSource source) {
        return CandidateResponse.from(saveNewCandidate(request, actor.getId(), source));
    }

    @Transactional(readOnly = true)
    public CandidateListResponse list(String tag) {
        List<Candidate> candidates = (tag == null || tag.isBlank())
                ? candidateRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()
                : candidateRepository.findByTagAndDeletedAtIsNull(tag.trim());
        return new CandidateListResponse(candidates.stream().map(CandidateResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public CandidateResponse getById(UUID id) {
        Candidate candidate = requireCandidate(id);
        List<CandidateDocument> documents = documentRepository.findByCandidateIdOrderByUploadedAtDesc(id);
        return CandidateResponse.from(candidate, documents);
    }

    @Transactional
    public CandidateResponse update(UUID id, UpdateCandidateRequest request) {
        Candidate candidate = requireCandidate(id);
        if (request.firstName() != null && !request.firstName().isBlank()) {
            candidate.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            candidate.setLastName(request.lastName().trim());
        }
        if (request.email() != null && !request.email().isBlank()) {
            String email = request.email().trim().toLowerCase(Locale.ROOT);
            if (candidateRepository.existsByEmailIgnoreCaseAndDeletedAtIsNullAndIdNot(email, id)) {
                throw new DuplicateCandidateEmailException(email);
            }
            candidate.setEmail(email);
        }
        if (request.phone() != null) {
            candidate.setPhone(request.phone().isBlank() ? null : request.phone().trim());
        }
        if (request.talentStatus() != null) {
            candidate.setTalentStatus(request.talentStatus());
        }
        if (request.tags() != null) {
            candidate.setTags(resolveTags(request.tags()));
        }
        return CandidateResponse.from(candidateRepository.save(candidate));
    }

    @Transactional
    public void delete(UUID id) {
        Candidate candidate = requireCandidate(id);
        candidate.setDeletedAt(Instant.now());
        candidate.setTalentStatus(TalentStatus.ARCHIVED);
        // Free the unique email so a new candidate can reuse the address after soft-delete.
        String suffix = "#deleted#" + candidate.getId();
        String email = candidate.getEmail();
        if (email.length() + suffix.length() > 255) {
            email = email.substring(0, Math.max(0, 255 - suffix.length()));
        }
        candidate.setEmail(email + suffix);
        candidateRepository.save(candidate);
    }

    @Transactional
    public CvUploadResponse uploadCv(UUID id, MultipartFile file, UserPrincipal actor) {
        Candidate candidate = requireCandidate(id);
        ParsedCvData parsed = cvParseService.parse(file);
        FileStorageService.StoredFile stored = fileStorageService.storeCv(id, file);

        CandidateDocument document = new CandidateDocument();
        document.setCandidate(candidate);
        document.setDocumentType(DocumentType.CV);
        document.setOriginalFilename(stored.originalFilename());
        document.setStoragePath(stored.storagePath());
        document.setContentType(stored.contentType());
        document.setSizeBytes(stored.sizeBytes());
        document.setUploadedByUserId(actor.getId());
        document = documentRepository.save(document);

        applyParsedFieldsIfEmpty(candidate, parsed);
        candidate = candidateRepository.save(candidate);

        return new CvUploadResponse(
                DocumentResponse.from(document),
                CandidateResponse.from(candidate, List.of(document)),
                parsed
        );
    }

    public BulkCvUploadResponse uploadCvBulk(MultipartFile[] files, UserPrincipal actor) {
        if (files == null || files.length == 0) {
            throw new InvalidFileException("At least one CV file is required");
        }

        List<BulkCvUploadItemResponse> items = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null) {
                continue;
            }
            String filename = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                    ? "unknown"
                    : file.getOriginalFilename();
            // Skip blank placeholder parts Postman sometimes sends
            if ((file.getOriginalFilename() == null || file.getOriginalFilename().isBlank())
                    && file.getSize() <= 0) {
                continue;
            }
            try {
                BulkCvUploadItemResponse item = transactionTemplate.execute(status -> processBulkFile(file, actor));
                items.add(item);
            } catch (DuplicateCandidateEmailException | InvalidFileException ex) {
                items.add(BulkCvUploadItemResponse.fail(filename, ex.getMessage()));
            } catch (Exception ex) {
                items.add(BulkCvUploadItemResponse.fail(filename, "Could not process CV"));
            }
        }
        if (items.isEmpty()) {
            throw new InvalidFileException(
                    "No CV files received. In Postman: Body → form-data → key must be 'files', type File, then Select Files."
            );
        }
        return new BulkCvUploadResponse(items);
    }

    private BulkCvUploadItemResponse processBulkFile(MultipartFile file, UserPrincipal actor) {
        String filename = file.getOriginalFilename() == null ? "cv" : file.getOriginalFilename();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception ex) {
            throw new InvalidFileException("Could not read CV file: " + filename);
        }
        if (bytes.length == 0) {
            throw new InvalidFileException(
                    "CV file is empty (0 bytes): " + filename
                            + ". In Postman use Body → form-data → key 'files' with type File (not Text), then Select Files."
            );
        }

        String extension = filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
                : "";
        ParsedCvData parsed = cvParseService.parseBytes(bytes, extension);
        if (parsed.email() == null || parsed.email().isBlank()) {
            throw new InvalidFileException("Could not find an email address in the CV");
        }
        if (parsed.firstName() == null || parsed.firstName().isBlank()
                || parsed.lastName() == null || parsed.lastName().isBlank()) {
            throw new InvalidFileException("Could not find a name in the CV");
        }

        CreateCandidateRequest request = new CreateCandidateRequest(
                parsed.firstName(),
                parsed.lastName(),
                parsed.email(),
                parsed.phone(),
                parsed.tags()
        );
        Candidate candidate = saveNewCandidate(request, actor.getId(), CandidateSource.CV_PARSE);
        FileStorageService.StoredFile stored = fileStorageService.storeCv(
                candidate.getId(),
                filename,
                bytes,
                file.getContentType()
        );

        CandidateDocument document = new CandidateDocument();
        document.setCandidate(candidate);
        document.setDocumentType(DocumentType.CV);
        document.setOriginalFilename(stored.originalFilename());
        document.setStoragePath(stored.storagePath());
        document.setContentType(stored.contentType());
        document.setSizeBytes(stored.sizeBytes());
        document.setUploadedByUserId(actor.getId());
        document = documentRepository.save(document);

        return BulkCvUploadItemResponse.ok(
                filename,
                CandidateResponse.from(candidate, List.of(document)),
                DocumentResponse.from(document),
                parsed
        );
    }

    private void applyParsedFieldsIfEmpty(Candidate candidate, ParsedCvData parsed) {
        if (parsed == null) {
            return;
        }
        if (isPlaceholderName(candidate.getFirstName()) && parsed.firstName() != null && !parsed.firstName().isBlank()) {
            candidate.setFirstName(parsed.firstName().trim());
        }
        if (isPlaceholderName(candidate.getLastName()) && parsed.lastName() != null && !parsed.lastName().isBlank()) {
            candidate.setLastName(parsed.lastName().trim());
        }
        if ((candidate.getPhone() == null || candidate.getPhone().isBlank())
                && parsed.phone() != null && !parsed.phone().isBlank()) {
            candidate.setPhone(parsed.phone().trim());
        }
        if (parsed.tags() != null && !parsed.tags().isEmpty()) {
            Set<Tag> merged = new LinkedHashSet<>(candidate.getTags());
            merged.addAll(resolveTags(parsed.tags()));
            candidate.setTags(merged);
        }
    }

    private static boolean isPlaceholderName(String value) {
        return value == null || value.isBlank() || "Unknown".equalsIgnoreCase(value) || "TBD".equalsIgnoreCase(value);
    }

    private Candidate saveNewCandidate(CreateCandidateRequest request, UUID createdByUserId, CandidateSource source) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (candidateRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            throw new DuplicateCandidateEmailException(email);
        }
        Candidate candidate = new Candidate();
        candidate.setFirstName(request.firstName().trim());
        candidate.setLastName(request.lastName().trim());
        candidate.setEmail(email);
        candidate.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        candidate.setSource(source);
        candidate.setTalentStatus(TalentStatus.IN_POOL);
        candidate.setCreatedByUserId(createdByUserId);
        candidate.setTags(resolveTags(request.tags()));
        return candidateRepository.save(candidate);
    }

    private Candidate requireCandidate(UUID id) {
        return candidateRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CandidateNotFoundException(id.toString()));
    }

    private Set<Tag> resolveTags(List<String> rawTags) {
        Set<Tag> tags = new HashSet<>();
        if (rawTags == null) {
            return tags;
        }
        for (String raw : rawTags) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String name = raw.trim().toLowerCase(Locale.ROOT);
            Tag tag = tagRepository.findByNameIgnoreCase(name).orElseGet(() -> {
                Tag created = new Tag();
                created.setName(name);
                return tagRepository.save(created);
            });
            tags.add(tag);
        }
        return tags;
    }
}
