package com.recruitment.applicationservice.service;

import com.recruitment.applicationservice.client.AuthServiceClient;
import com.recruitment.applicationservice.client.CandidateServiceClient;
import com.recruitment.applicationservice.domain.Application;
import com.recruitment.applicationservice.domain.ApplicationAssignment;
import com.recruitment.applicationservice.domain.ApplicationStageEvent;
import com.recruitment.applicationservice.domain.Evaluation;
import com.recruitment.applicationservice.domain.Job;
import com.recruitment.applicationservice.domain.JobStatus;
import com.recruitment.applicationservice.domain.PipelineStage;
import com.recruitment.applicationservice.dto.ApplicationResponse;
import com.recruitment.applicationservice.dto.CreateApplicationRequest;
import com.recruitment.applicationservice.dto.StageChangeRequest;
import com.recruitment.applicationservice.exception.BadRequestException;
import com.recruitment.applicationservice.repository.ApplicationAssignmentRepository;
import com.recruitment.applicationservice.repository.ApplicationRepository;
import com.recruitment.applicationservice.repository.ApplicationStageEventRepository;
import com.recruitment.applicationservice.repository.EvaluationRepository;
import com.recruitment.applicationservice.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationTrackingServiceTest {

    private static final UUID JOB_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CANDIDATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID APPLICATION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String AUTH = "Bearer test-token";

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ApplicationStageEventRepository stageEventRepository;
    @Mock
    private ApplicationAssignmentRepository assignmentRepository;
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private JobService jobService;
    @Mock
    private CandidateServiceClient candidateServiceClient;
    @Mock
    private AuthServiceClient authServiceClient;

    private ApplicationTrackingService applicationTrackingService;

    private final UserPrincipal hr = new UserPrincipal(
            UUID.fromString("44444444-4444-4444-4444-444444444444"),
            "hr@company.com",
            "HR"
    );

    @BeforeEach
    void setUp() {
        applicationTrackingService = new ApplicationTrackingService(
                applicationRepository,
                stageEventRepository,
                assignmentRepository,
                evaluationRepository,
                jobService,
                candidateServiceClient,
                authServiceClient
        );
    }

    @Test
    void createValidatesCandidateExistsBeforeSaving() {
        Job publishedJob = publishedJob();
        when(jobService.requireJob(JOB_ID)).thenReturn(publishedJob);
        when(applicationRepository.existsByJobIdAndCandidateId(JOB_ID, CANDIDATE_ID)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> {
            Application application = invocation.getArgument(0);
            setId(application, APPLICATION_ID);
            return application;
        });
        when(stageEventRepository.save(any(ApplicationStageEvent.class))).thenAnswer(invocation -> {
            ApplicationStageEvent event = invocation.getArgument(0);
            setStageEventId(event, UUID.fromString("55555555-5555-5555-5555-555555555555"));
            return event;
        });

        ApplicationResponse response = applicationTrackingService.create(
                new CreateApplicationRequest(JOB_ID, CANDIDATE_ID),
                hr,
                AUTH
        );

        verify(candidateServiceClient).requireCandidateExists(CANDIDATE_ID, AUTH);
        assertThat(response.id()).isEqualTo(APPLICATION_ID);
        assertThat(response.currentStage()).isEqualTo(PipelineStage.APPLIED);
        assertThat(response.candidateId()).isEqualTo(CANDIDATE_ID);
    }

    @Test
    void createDoesNotSaveWhenCandidateMissing() {
        Job publishedJob = publishedJob();
        when(jobService.requireJob(JOB_ID)).thenReturn(publishedJob);
        doThrow(new BadRequestException("Candidate not found: " + CANDIDATE_ID))
                .when(candidateServiceClient).requireCandidateExists(CANDIDATE_ID, AUTH);

        assertThatThrownBy(() -> applicationTrackingService.create(
                new CreateApplicationRequest(JOB_ID, CANDIDATE_ID),
                hr,
                AUTH
        )).isInstanceOf(BadRequestException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void changeStageToHiredCallsCandidateService() {
        Application application = application(PipelineStage.OFFER);
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stageEventRepository.save(any(ApplicationStageEvent.class))).thenAnswer(invocation -> {
            ApplicationStageEvent event = invocation.getArgument(0);
            setStageEventId(event, UUID.fromString("66666666-6666-6666-6666-666666666666"));
            return event;
        });

        applicationTrackingService.changeStage(
                APPLICATION_ID,
                new StageChangeRequest(PipelineStage.HIRED, "Offer accepted"),
                hr,
                AUTH
        );

        verify(candidateServiceClient).markHired(CANDIDATE_ID, AUTH);
    }

    @Test
    void deleteRemovesEvaluationsAssignmentsStageEventsAndApplication() {
        Application application = application(PipelineStage.INTERVIEW);
        Evaluation evaluation = new Evaluation();
        ApplicationAssignment assignment = new ApplicationAssignment();
        ApplicationStageEvent stageEvent = new ApplicationStageEvent();

        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(evaluationRepository.findByApplicationIdOrderByCreatedAtDesc(APPLICATION_ID))
                .thenReturn(List.of(evaluation));
        when(assignmentRepository.findByApplicationIdOrderByCreatedAtAsc(APPLICATION_ID))
                .thenReturn(List.of(assignment));
        when(stageEventRepository.findByApplicationIdOrderByCreatedAtAsc(APPLICATION_ID))
                .thenReturn(List.of(stageEvent));

        applicationTrackingService.delete(APPLICATION_ID);

        verify(evaluationRepository).deleteAll(eq(List.of(evaluation)));
        verify(assignmentRepository).deleteAll(eq(List.of(assignment)));
        verify(stageEventRepository).deleteAll(eq(List.of(stageEvent)));
        verify(applicationRepository).delete(application);
    }

    private static Job publishedJob() {
        Job job = new Job();
        setId(job, JOB_ID);
        job.setTitle("Java Engineer");
        job.setJobStatus(JobStatus.PUBLISHED);
        job.setCreatedByUserId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        job.setEmploymentType(com.recruitment.applicationservice.domain.EmploymentType.FULL_TIME);
        return job;
    }

    private Application application(PipelineStage stage) {
        Application application = new Application();
        setId(application, APPLICATION_ID);
        application.setJob(publishedJob());
        application.setCandidateId(CANDIDATE_ID);
        application.setCurrentStage(stage);
        return application;
    }

    private static void setId(Application application, UUID id) {
        setField(application, id);
    }

    private static void setId(Job job, UUID id) {
        try {
            var field = Job.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(job, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setStageEventId(ApplicationStageEvent event, UUID id) {
        try {
            var field = ApplicationStageEvent.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(event, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setField(Application application, UUID id) {
        try {
            var field = Application.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(application, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
