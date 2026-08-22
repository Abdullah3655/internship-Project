package com.recruitment.applicationservice.repository;

import com.recruitment.applicationservice.domain.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    List<Evaluation> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);

    Optional<Evaluation> findByIdAndApplicationId(UUID id, UUID applicationId);
}
