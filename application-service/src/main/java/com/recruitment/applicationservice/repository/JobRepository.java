package com.recruitment.applicationservice.repository;

import com.recruitment.applicationservice.domain.Job;
import com.recruitment.applicationservice.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findAllByOrderByCreatedAtDesc();

    List<Job> findByJobStatusOrderByCreatedAtDesc(JobStatus jobStatus);

    @Query("""
            select distinct j from Job j
            join j.tags t
            where lower(t.name) = lower(:tag)
            order by j.createdAt desc
            """)
    List<Job> findByTag(@Param("tag") String tag);

    @Query("""
            select distinct j from Job j
            join j.tags t
            where j.jobStatus = :status and lower(t.name) = lower(:tag)
            order by j.createdAt desc
            """)
    List<Job> findByJobStatusAndTag(@Param("status") JobStatus status, @Param("tag") String tag);
}
