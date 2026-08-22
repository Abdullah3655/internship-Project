package com.recruitment.applicationservice.repository;

import com.recruitment.applicationservice.domain.ApplicationAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationAssignmentRepository extends JpaRepository<ApplicationAssignment, UUID> {

    List<ApplicationAssignment> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);

    List<ApplicationAssignment> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<ApplicationAssignment> findByIdAndApplicationId(UUID id, UUID applicationId);

    boolean existsByApplicationIdAndUserIdAndAssignmentRole(
            UUID applicationId,
            UUID userId,
            com.recruitment.applicationservice.domain.AssignmentRole assignmentRole
    );
}
