package com.recruitment.applicationservice.repository;

import com.recruitment.applicationservice.domain.Application;
import com.recruitment.applicationservice.domain.PipelineStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    boolean existsByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    boolean existsByJobId(UUID jobId);

    @Query("""
            select a from Application a
            join fetch a.job
            where a.id = :id
            """)
    Optional<Application> findByIdWithJob(@Param("id") UUID id);

    @Query("""
            select distinct a from Application a
            join fetch a.job
            where a.job.id = :jobId
            order by a.createdAt desc
            """)
    List<Application> findByJobIdOrderByCreatedAtDesc(@Param("jobId") UUID jobId);

    @Query("""
            select distinct a from Application a
            join fetch a.job
            where a.job.id = :jobId and a.currentStage = :stage
            order by a.createdAt desc
            """)
    List<Application> findByJobIdAndCurrentStageOrderByCreatedAtDesc(
            @Param("jobId") UUID jobId,
            @Param("stage") PipelineStage stage
    );

    @Query("""
            select distinct a from Application a
            join fetch a.job
            where a.candidateId = :candidateId
            order by a.createdAt desc
            """)
    List<Application> findByCandidateIdOrderByCreatedAtDesc(@Param("candidateId") UUID candidateId);

    @Query("""
            select distinct a from Application a
            join fetch a.job
            where a.candidateId = :candidateId and a.currentStage = :stage
            order by a.createdAt desc
            """)
    List<Application> findByCandidateIdAndCurrentStageOrderByCreatedAtDesc(
            @Param("candidateId") UUID candidateId,
            @Param("stage") PipelineStage stage
    );

    @Query("""
            select distinct a from Application a
            join fetch a.job
            where a.currentStage = :stage
            order by a.createdAt desc
            """)
    List<Application> findByCurrentStageOrderByCreatedAtDesc(@Param("stage") PipelineStage stage);

    @Query("""
            select distinct a from Application a
            join fetch a.job
            order by a.createdAt desc
            """)
    List<Application> findAllByOrderByCreatedAtDesc();

    @Query("""
            select distinct a from Application a
            join fetch a.job
            join ApplicationAssignment aa on aa.application = a
            where aa.userId = :userId
            order by a.createdAt desc
            """)
    List<Application> findAssignedToUser(@Param("userId") UUID userId);
}
