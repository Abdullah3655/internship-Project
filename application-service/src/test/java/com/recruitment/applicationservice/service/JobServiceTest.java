package com.recruitment.applicationservice.service;

import com.recruitment.applicationservice.domain.EmploymentType;
import com.recruitment.applicationservice.domain.Job;
import com.recruitment.applicationservice.domain.JobStatus;
import com.recruitment.applicationservice.dto.CreateJobRequest;
import com.recruitment.applicationservice.dto.JobResponse;
import com.recruitment.applicationservice.repository.JobRepository;
import com.recruitment.applicationservice.repository.TagRepository;
import com.recruitment.applicationservice.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private JobService jobService;

    @Test
    void createStartsAsDraftWithTags() {
        UserPrincipal hr = new UserPrincipal(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "hr@company.com",
                "HR"
        );
        when(tagRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(tagRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            setId(job, UUID.fromString("22222222-2222-2222-2222-222222222222"));
            return job;
        });

        JobResponse response = jobService.create(
                new CreateJobRequest(
                        "Java Engineer",
                        "Engineering",
                        "Remote",
                        "Build APIs",
                        EmploymentType.FULL_TIME,
                        List.of("Java", "Spring")
                ),
                hr
        );

        assertThat(response.jobStatus()).isEqualTo(JobStatus.DRAFT);
        assertThat(response.title()).isEqualTo("Java Engineer");
        assertThat(response.createdByUserId()).isEqualTo(hr.getId());
        assertThat(response.tags()).containsExactly("java", "spring");
    }

    @Test
    void createRejectsInvalidTag() {
        UserPrincipal hr = new UserPrincipal(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "hr@company.com",
                "HR"
        );

        assertThatThrownBy(() -> jobService.create(
                new CreateJobRequest(
                        "Java Engineer",
                        "Engineering",
                        "Remote",
                        "Build APIs",
                        EmploymentType.FULL_TIME,
                        List.of("Invalid Tag!")
                ),
                hr
        )).isInstanceOf(com.recruitment.applicationservice.exception.BadRequestException.class);
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
}
