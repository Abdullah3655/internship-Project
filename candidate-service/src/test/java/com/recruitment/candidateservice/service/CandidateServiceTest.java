package com.recruitment.candidateservice.service;

import com.recruitment.candidateservice.domain.Candidate;
import com.recruitment.candidateservice.domain.CandidateDocument;
import com.recruitment.candidateservice.domain.CandidateSource;
import com.recruitment.candidateservice.domain.TalentStatus;
import com.recruitment.candidateservice.dto.BulkCvUploadResponse;
import com.recruitment.candidateservice.dto.CandidateResponse;
import com.recruitment.candidateservice.dto.CreateCandidateRequest;
import com.recruitment.candidateservice.dto.CvUploadResponse;
import com.recruitment.candidateservice.dto.ParsedCvData;
import com.recruitment.candidateservice.exception.DuplicateCandidateEmailException;
import com.recruitment.candidateservice.repository.CandidateDocumentRepository;
import com.recruitment.candidateservice.repository.CandidateRepository;
import com.recruitment.candidateservice.repository.TagRepository;
import com.recruitment.candidateservice.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CandidateServiceTest {

    @Mock
    private CandidateRepository candidateRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private CandidateDocumentRepository documentRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private CvParseService cvParseService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private CandidateService candidateService;

    private final UserPrincipal hr = new UserPrincipal(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "hr@company.com",
            "HR"
    );

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
        candidateService = new CandidateService(
                candidateRepository,
                tagRepository,
                documentRepository,
                fileStorageService,
                cvParseService,
                transactionManager
        );
    }

    @Test
    void createSavesCandidateInPool() {
        CreateCandidateRequest request = new CreateCandidateRequest(
                "Alice", "Smith", "alice@example.com", "+123", List.of("Java")
        );
        when(candidateRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("alice@example.com")).thenReturn(false);
        when(tagRepository.findByNameIgnoreCase("java")).thenReturn(Optional.empty());
        when(tagRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> {
            Candidate candidate = invocation.getArgument(0);
            setId(candidate, UUID.fromString("22222222-2222-2222-2222-222222222222"));
            return candidate;
        });

        CandidateResponse response = candidateService.create(request, hr, CandidateSource.MANUAL);

        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.talentStatus()).isEqualTo(TalentStatus.IN_POOL);
        assertThat(response.source()).isEqualTo(CandidateSource.MANUAL);
        assertThat(response.createdByUserId()).isEqualTo(hr.getId());
        assertThat(response.tags()).contains("java");
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(candidateRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> candidateService.create(
                new CreateCandidateRequest("Alice", "Smith", "alice@example.com", null, List.of()),
                hr,
                CandidateSource.MANUAL
        )).isInstanceOf(DuplicateCandidateEmailException.class);
    }

    @Test
    void uploadCvParsesAndFillsEmptyPhoneAndTags() {
        UUID candidateId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Candidate candidate = new Candidate();
        setId(candidate, candidateId);
        candidate.setFirstName("Alice");
        candidate.setLastName("Smith");
        candidate.setEmail("alice@example.com");
        candidate.setPhone(null);
        candidate.setSource(CandidateSource.MANUAL);
        candidate.setTalentStatus(TalentStatus.IN_POOL);
        candidate.setCreatedByUserId(hr.getId());

        MockMultipartFile file = new MockMultipartFile(
                "file", "alice.txt", "text/plain", "cv".getBytes(StandardCharsets.UTF_8)
        );
        ParsedCvData parsed = new ParsedCvData(
                "Alice", "Smith", "alice@example.com", "+15551234567", List.of("java"), "preview"
        );

        when(candidateRepository.findByIdAndDeletedAtIsNull(candidateId)).thenReturn(Optional.of(candidate));
        when(cvParseService.parse(file)).thenReturn(parsed);
        when(fileStorageService.storeCv(candidateId, file)).thenReturn(
                new FileStorageService.StoredFile("alice.txt", "uploads/alice.txt", "text/plain", 2)
        );
        when(documentRepository.save(any(CandidateDocument.class))).thenAnswer(invocation -> {
            CandidateDocument document = invocation.getArgument(0);
            setDocumentId(document, UUID.fromString("33333333-3333-3333-3333-333333333333"));
            return document;
        });
        when(tagRepository.findByNameIgnoreCase("java")).thenReturn(Optional.empty());
        when(tagRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CvUploadResponse response = candidateService.uploadCv(candidateId, file, hr);

        assertThat(response.parsed().email()).isEqualTo("alice@example.com");
        assertThat(response.candidate().phone()).isEqualTo("+15551234567");
        assertThat(response.candidate().tags()).contains("java");
        assertThat(response.document().originalFilename()).isEqualTo("alice.txt");
    }

    @Test
    void uploadCvBulkSucceedsForValidTxtAndFailsInvalidType() {
        MockMultipartFile good = new MockMultipartFile(
                "files",
                "bob.txt",
                "text/plain",
                """
                        Bob Jones
                        bob@example.com
                        Skills: Python
                        """.getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile bad = new MockMultipartFile(
                "files",
                "notes.exe",
                "application/octet-stream",
                "x".getBytes(StandardCharsets.UTF_8)
        );

        when(cvParseService.parseBytes(any(byte[].class), org.mockito.ArgumentMatchers.eq("txt"))).thenReturn(new ParsedCvData(
                "Bob", "Jones", "bob@example.com", null, List.of("python"), "preview"
        ));
        when(cvParseService.parseBytes(any(byte[].class), org.mockito.ArgumentMatchers.eq("exe")))
                .thenThrow(new com.recruitment.candidateservice.exception.InvalidFileException(
                        "CV must be a pdf, doc, docx, or txt file"
                ));
        when(candidateRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull("bob@example.com")).thenReturn(false);
        when(tagRepository.findByNameIgnoreCase("python")).thenReturn(Optional.empty());
        when(tagRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> {
            Candidate candidate = invocation.getArgument(0);
            setId(candidate, UUID.fromString("44444444-4444-4444-4444-444444444444"));
            return candidate;
        });
        when(fileStorageService.storeCv(any(), any(), any(byte[].class), any())).thenReturn(
                new FileStorageService.StoredFile("bob.txt", "uploads/bob.txt", "text/plain", 10)
        );
        when(documentRepository.save(any(CandidateDocument.class))).thenAnswer(invocation -> {
            CandidateDocument document = invocation.getArgument(0);
            setDocumentId(document, UUID.fromString("55555555-5555-5555-5555-555555555555"));
            return document;
        });

        BulkCvUploadResponse response = candidateService.uploadCvBulk(new MockMultipartFile[]{good, bad}, hr);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).success()).isTrue();
        assertThat(response.items().get(0).candidate().email()).isEqualTo("bob@example.com");
        assertThat(response.items().get(1).success()).isFalse();
        assertThat(response.items().get(1).error()).contains("pdf");
    }

    private static void setId(Candidate candidate, UUID id) {
        try {
            var field = Candidate.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(candidate, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setDocumentId(CandidateDocument document, UUID id) {
        try {
            var field = CandidateDocument.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(document, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
