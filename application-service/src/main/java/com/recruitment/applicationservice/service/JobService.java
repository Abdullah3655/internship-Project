package com.recruitment.applicationservice.service;

import com.recruitment.applicationservice.domain.Job;
import com.recruitment.applicationservice.domain.JobStatus;
import com.recruitment.applicationservice.domain.Tag;
import com.recruitment.applicationservice.dto.CreateJobRequest;
import com.recruitment.applicationservice.dto.JobListResponse;
import com.recruitment.applicationservice.dto.JobResponse;
import com.recruitment.applicationservice.dto.UpdateJobRequest;
import com.recruitment.applicationservice.exception.BadRequestException;
import com.recruitment.applicationservice.exception.ResourceNotFoundException;
import com.recruitment.applicationservice.repository.JobRepository;
import com.recruitment.applicationservice.repository.TagRepository;
import com.recruitment.applicationservice.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final TagRepository tagRepository;

    public JobService(JobRepository jobRepository, TagRepository tagRepository) {
        this.jobRepository = jobRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional
    public JobResponse create(CreateJobRequest request, UserPrincipal actor) {
        Job job = new Job();
        job.setTitle(request.title().trim());
        job.setDepartment(blankToNull(request.department()));
        job.setLocation(blankToNull(request.location()));
        job.setDescription(blankToNull(request.description()));
        job.setEmploymentType(request.employmentType());
        job.setJobStatus(JobStatus.DRAFT);
        job.setCreatedByUserId(actor.getId());
        job.setTags(resolveTags(request.tags()));
        return JobResponse.from(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public JobListResponse list(JobStatus status, String tag) {
        List<Job> jobs;
        boolean hasTag = tag != null && !tag.isBlank();
        if (hasTag && status != null) {
            jobs = jobRepository.findByJobStatusAndTag(status, tag.trim());
        } else if (hasTag) {
            jobs = jobRepository.findByTag(tag.trim());
        } else if (status != null) {
            jobs = jobRepository.findByJobStatusOrderByCreatedAtDesc(status);
        } else {
            jobs = jobRepository.findAllByOrderByCreatedAtDesc();
        }
        return new JobListResponse(jobs.stream().map(JobResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public JobResponse getById(UUID id) {
        return JobResponse.from(requireJob(id));
    }

    @Transactional
    public JobResponse update(UUID id, UpdateJobRequest request) {
        Job job = requireJob(id);
        if (request.title() != null && !request.title().isBlank()) {
            job.setTitle(request.title().trim());
        }
        if (request.department() != null) {
            job.setDepartment(blankToNull(request.department()));
        }
        if (request.location() != null) {
            job.setLocation(blankToNull(request.location()));
        }
        if (request.description() != null) {
            job.setDescription(blankToNull(request.description()));
        }
        if (request.employmentType() != null) {
            job.setEmploymentType(request.employmentType());
        }
        if (request.jobStatus() != null) {
            if (request.jobStatus() == JobStatus.PUBLISHED && job.getPublishedAt() == null) {
                job.setPublishedAt(Instant.now());
            }
            job.setJobStatus(request.jobStatus());
        }
        if (request.tags() != null) {
            job.setTags(resolveTags(request.tags()));
        }
        return JobResponse.from(jobRepository.save(job));
    }

    @Transactional
    public JobResponse publish(UUID id) {
        Job job = requireJob(id);
        if (job.getJobStatus() == JobStatus.CLOSED) {
            throw new BadRequestException("Closed jobs cannot be published");
        }
        if (job.getJobStatus() == JobStatus.PUBLISHED) {
            return JobResponse.from(job);
        }
        job.setJobStatus(JobStatus.PUBLISHED);
        job.setPublishedAt(Instant.now());
        return JobResponse.from(jobRepository.save(job));
    }

    @Transactional
    public void delete(UUID id) {
        Job job = requireJob(id);
        jobRepository.delete(job);
    }

    public Job requireJob(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + id));
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

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
