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
import com.recruitment.applicationservice.dto.ApplicationListResponse;
import com.recruitment.applicationservice.dto.ApplicationResponse;
import com.recruitment.applicationservice.dto.AssignmentListResponse;
import com.recruitment.applicationservice.dto.AssignmentResponse;
import com.recruitment.applicationservice.dto.CreateApplicationRequest;
import com.recruitment.applicationservice.dto.CreateAssignmentRequest;
import com.recruitment.applicationservice.dto.CreateEvaluationRequest;
import com.recruitment.applicationservice.dto.EvaluationListResponse;
import com.recruitment.applicationservice.dto.EvaluationResponse;
import com.recruitment.applicationservice.dto.StageChangeRequest;
import com.recruitment.applicationservice.dto.StageEventListResponse;
import com.recruitment.applicationservice.dto.StageEventResponse;
import com.recruitment.applicationservice.exception.BadRequestException;
import com.recruitment.applicationservice.exception.ConflictException;
import com.recruitment.applicationservice.exception.ResourceNotFoundException;
import com.recruitment.applicationservice.repository.ApplicationAssignmentRepository;
import com.recruitment.applicationservice.repository.ApplicationRepository;
import com.recruitment.applicationservice.repository.ApplicationStageEventRepository;
import com.recruitment.applicationservice.repository.EvaluationRepository;
import com.recruitment.applicationservice.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ApplicationTrackingService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStageEventRepository stageEventRepository;
    private final ApplicationAssignmentRepository assignmentRepository;
    private final EvaluationRepository evaluationRepository;
    private final JobService jobService;
    private final CandidateServiceClient candidateServiceClient;
    private final AuthServiceClient authServiceClient;

    public ApplicationTrackingService(
            ApplicationRepository applicationRepository,
            ApplicationStageEventRepository stageEventRepository,
            ApplicationAssignmentRepository assignmentRepository,
            EvaluationRepository evaluationRepository,
            JobService jobService,
            CandidateServiceClient candidateServiceClient,
            AuthServiceClient authServiceClient
    ) {
        this.applicationRepository = applicationRepository;
        this.stageEventRepository = stageEventRepository;
        this.assignmentRepository = assignmentRepository;
        this.evaluationRepository = evaluationRepository;
        this.jobService = jobService;
        this.candidateServiceClient = candidateServiceClient;
        this.authServiceClient = authServiceClient;
    }

    @Transactional
    public ApplicationResponse create(CreateApplicationRequest request, UserPrincipal actor, String authorization) {
        Job job = jobService.requireJob(request.jobId());
        if (job.getJobStatus() != JobStatus.PUBLISHED) {
            throw new BadRequestException("Applications can only be created for published jobs");
        }
        candidateServiceClient.requireCandidateExists(request.candidateId(), authorization);
        if (applicationRepository.existsByJobIdAndCandidateId(request.jobId(), request.candidateId())) {
            throw new ConflictException("Candidate already applied to this job");
        }

        Application application = new Application();
        application.setJob(job);
        application.setCandidateId(request.candidateId());
        application.setCurrentStage(PipelineStage.APPLIED);
        application = applicationRepository.save(application);

        ApplicationStageEvent event = new ApplicationStageEvent();
        event.setApplication(application);
        event.setFromStage(null);
        event.setToStage(PipelineStage.APPLIED);
        event.setChangedByUserId(actor.getId());
        event.setNote("Application created");
        stageEventRepository.save(event);

        return ApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public ApplicationListResponse list(UUID jobId, UUID candidateId, PipelineStage stage) {
        List<Application> applications;
        if (jobId != null && stage != null) {
            applications = applicationRepository.findByJobIdAndCurrentStageOrderByCreatedAtDesc(jobId, stage);
        } else if (candidateId != null && stage != null) {
            applications = applicationRepository.findByCandidateIdAndCurrentStageOrderByCreatedAtDesc(candidateId, stage);
        } else if (jobId != null) {
            applications = applicationRepository.findByJobIdOrderByCreatedAtDesc(jobId);
        } else if (candidateId != null) {
            applications = applicationRepository.findByCandidateIdOrderByCreatedAtDesc(candidateId);
        } else if (stage != null) {
            applications = applicationRepository.findByCurrentStageOrderByCreatedAtDesc(stage);
        } else {
            applications = applicationRepository.findAllByOrderByCreatedAtDesc();
        }
        return new ApplicationListResponse(applications.stream().map(ApplicationResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getById(UUID id) {
        return ApplicationResponse.from(requireApplication(id));
    }

    @Transactional
    public StageEventResponse changeStage(
            UUID id,
            StageChangeRequest request,
            UserPrincipal actor,
            String authorization
    ) {
        Application application = requireApplication(id);
        PipelineStage from = application.getCurrentStage();
        PipelineStage to = request.toStage();
        if (from == to) {
            throw new BadRequestException("Application is already in stage " + to);
        }

        application.setCurrentStage(to);
        applicationRepository.save(application);

        ApplicationStageEvent event = new ApplicationStageEvent();
        event.setApplication(application);
        event.setFromStage(from);
        event.setToStage(to);
        event.setChangedByUserId(actor.getId());
        event.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
        StageEventResponse response = StageEventResponse.from(stageEventRepository.save(event));

        if (to == PipelineStage.HIRED) {
            candidateServiceClient.markHired(application.getCandidateId(), authorization);
        }

        return response;
    }

    @Transactional
    public void delete(UUID id) {
        Application application = requireApplication(id);
        UUID applicationId = application.getId();
        evaluationRepository.deleteAll(evaluationRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId));
        assignmentRepository.deleteAll(assignmentRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId));
        stageEventRepository.deleteAll(stageEventRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId));
        applicationRepository.delete(application);
    }

    @Transactional(readOnly = true)
    public StageEventListResponse listStageChanges(UUID id) {
        requireApplication(id);
        return new StageEventListResponse(
                stageEventRepository.findByApplicationIdOrderByCreatedAtAsc(id).stream()
                        .map(StageEventResponse::from)
                        .toList()
        );
    }

    @Transactional
    public AssignmentResponse assign(UUID id, CreateAssignmentRequest request, String authorization) {
        Application application = requireApplication(id);
        authServiceClient.requireUserExists(request.userId(), authorization);
        if (assignmentRepository.existsByApplicationIdAndUserIdAndAssignmentRole(
                id, request.userId(), request.assignmentRole())) {
            throw new ConflictException("User already assigned with this role");
        }
        ApplicationAssignment assignment = new ApplicationAssignment();
        assignment.setApplication(application);
        assignment.setUserId(request.userId());
        assignment.setAssignmentRole(request.assignmentRole());
        return AssignmentResponse.from(assignmentRepository.save(assignment));
    }

    @Transactional
    public void deleteAssignment(UUID applicationId, UUID assignmentId) {
        requireApplication(applicationId);
        ApplicationAssignment assignment = assignmentRepository.findByIdAndApplicationId(assignmentId, applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
        assignmentRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public AssignmentListResponse listAssignments(UUID id) {
        requireApplication(id);
        return new AssignmentListResponse(
                assignmentRepository.findByApplicationIdOrderByCreatedAtAsc(id).stream()
                        .map(AssignmentResponse::from)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public AssignmentListResponse listMyAssignments(UserPrincipal actor) {
        return new AssignmentListResponse(
                assignmentRepository.findByUserIdOrderByCreatedAtDesc(actor.getId()).stream()
                        .map(AssignmentResponse::from)
                        .toList()
        );
    }

    @Transactional
    public EvaluationResponse evaluate(UUID id, CreateEvaluationRequest request, UserPrincipal actor) {
        Application application = requireApplication(id);
        Evaluation evaluation = new Evaluation();
        evaluation.setApplication(application);
        evaluation.setInterviewerUserId(actor.getId());
        evaluation.setScore(request.score());
        evaluation.setFeedback(request.feedback() == null || request.feedback().isBlank()
                ? null
                : request.feedback().trim());
        return EvaluationResponse.from(evaluationRepository.save(evaluation));
    }

    @Transactional
    public void deleteEvaluation(UUID applicationId, UUID evaluationId) {
        requireApplication(applicationId);
        Evaluation evaluation = evaluationRepository.findByIdAndApplicationId(evaluationId, applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found: " + evaluationId));
        evaluationRepository.delete(evaluation);
    }

    @Transactional(readOnly = true)
    public EvaluationListResponse listEvaluations(UUID id) {
        requireApplication(id);
        return new EvaluationListResponse(
                evaluationRepository.findByApplicationIdOrderByCreatedAtDesc(id).stream()
                        .map(EvaluationResponse::from)
                        .toList()
        );
    }

    private Application requireApplication(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + id));
    }
}
