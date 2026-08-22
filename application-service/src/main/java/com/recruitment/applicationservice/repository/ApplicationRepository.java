package com.recruitment.applicationservice.repository;

import com.recruitment.applicationservice.domain.Application;
import com.recruitment.applicationservice.domain.PipelineStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    boolean existsByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    boolean existsByJobId(UUID jobId);

    List<Application> findByJobIdOrderByCreatedAtDesc(UUID jobId);

    List<Application> findByJobIdAndCurrentStageOrderByCreatedAtDesc(UUID jobId, PipelineStage currentStage);

    List<Application> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId);

    List<Application> findByCandidateIdAndCurrentStageOrderByCreatedAtDesc(UUID candidateId, PipelineStage currentStage);

    List<Application> findByCurrentStageOrderByCreatedAtDesc(PipelineStage currentStage);

    List<Application> findAllByOrderByCreatedAtDesc();

    Optional<Application> findById(UUID id);
}
