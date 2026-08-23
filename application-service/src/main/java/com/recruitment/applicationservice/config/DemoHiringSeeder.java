package com.recruitment.applicationservice.config;

import com.recruitment.applicationservice.domain.Application;
import com.recruitment.applicationservice.domain.ApplicationAssignment;
import com.recruitment.applicationservice.domain.ApplicationStageEvent;
import com.recruitment.applicationservice.domain.AssignmentRole;
import com.recruitment.applicationservice.domain.EmploymentType;
import com.recruitment.applicationservice.domain.Job;
import com.recruitment.applicationservice.domain.JobStatus;
import com.recruitment.applicationservice.domain.PipelineStage;
import com.recruitment.applicationservice.domain.Tag;
import com.recruitment.applicationservice.repository.ApplicationAssignmentRepository;
import com.recruitment.applicationservice.repository.ApplicationRepository;
import com.recruitment.applicationservice.repository.ApplicationStageEventRepository;
import com.recruitment.applicationservice.repository.JobRepository;
import com.recruitment.applicationservice.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@Profile("!test")
public class DemoHiringSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoHiringSeeder.class);

    private final JobRepository jobRepository;
    private final TagRepository tagRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationAssignmentRepository assignmentRepository;
    private final ApplicationStageEventRepository stageEventRepository;

    public DemoHiringSeeder(
            JobRepository jobRepository,
            TagRepository tagRepository,
            ApplicationRepository applicationRepository,
            ApplicationAssignmentRepository assignmentRepository,
            ApplicationStageEventRepository stageEventRepository
    ) {
        this.jobRepository = jobRepository;
        this.tagRepository = tagRepository;
        this.applicationRepository = applicationRepository;
        this.assignmentRepository = assignmentRepository;
        this.stageEventRepository = stageEventRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Job job = seedPublishedJob();
        Application application = seedApplication(job);
        seedAssignment(application);
        log.info("Demo job ready: {} ({})", job.getTitle(), DemoIds.JAVA_JOB);
        log.info("Demo application ready: Alice -> Java Engineer at INTERVIEW ({})", DemoIds.ALICE_APPLICATION);
        log.info("Demo assignment ready: interviewer user {}", DemoIds.INTERVIEWER_USER);
    }

    private Job seedPublishedJob() {
        return jobRepository.findById(DemoIds.JAVA_JOB).orElseGet(() -> {
            Job job = new Job();
            job.setId(DemoIds.JAVA_JOB);
            job.setTitle("Java Engineer");
            job.setDepartment("Engineering");
            job.setLocation("Remote");
            job.setDescription("Build recruitment platform APIs with Spring Boot");
            job.setEmploymentType(EmploymentType.FULL_TIME);
            job.setJobStatus(JobStatus.PUBLISHED);
            job.setPublishedAt(Instant.now());
            job.setCreatedByUserId(DemoIds.HR_USER);
            job.setTags(resolveTags(List.of("java", "spring", "mysql")));
            return jobRepository.save(job);
        });
    }

    private Application seedApplication(Job job) {
        return applicationRepository.findById(DemoIds.ALICE_APPLICATION).orElseGet(() -> {
            Application application = new Application();
            application.setId(DemoIds.ALICE_APPLICATION);
            application.setJob(job);
            application.setCandidateId(DemoIds.ALICE_CANDIDATE);
            application.setCurrentStage(PipelineStage.INTERVIEW);
            application = applicationRepository.save(application);

            ApplicationStageEvent applied = new ApplicationStageEvent();
            applied.setId(DemoIds.ALICE_STAGE_APPLIED);
            applied.setApplication(application);
            applied.setFromStage(null);
            applied.setToStage(PipelineStage.APPLIED);
            applied.setChangedByUserId(DemoIds.HR_USER);
            applied.setNote("Application created");
            stageEventRepository.save(applied);

            ApplicationStageEvent interview = new ApplicationStageEvent();
            interview.setId(DemoIds.ALICE_STAGE_INTERVIEW);
            interview.setApplication(application);
            interview.setFromStage(PipelineStage.APPLIED);
            interview.setToStage(PipelineStage.INTERVIEW);
            interview.setChangedByUserId(DemoIds.HR_USER);
            interview.setNote("Passed screening");
            stageEventRepository.save(interview);

            return application;
        });
    }

    private void seedAssignment(Application application) {
        if (assignmentRepository.existsByApplicationIdAndUserIdAndAssignmentRole(
                application.getId(),
                DemoIds.INTERVIEWER_USER,
                AssignmentRole.INTERVIEWER
        )) {
            return;
        }
        ApplicationAssignment assignment = new ApplicationAssignment();
        assignment.setId(DemoIds.ALICE_ASSIGNMENT);
        assignment.setApplication(application);
        assignment.setUserId(DemoIds.INTERVIEWER_USER);
        assignment.setAssignmentRole(AssignmentRole.INTERVIEWER);
        assignmentRepository.save(assignment);
    }

    private Set<Tag> resolveTags(List<String> rawTags) {
        Set<Tag> tags = new LinkedHashSet<>();
        for (String raw : rawTags) {
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
