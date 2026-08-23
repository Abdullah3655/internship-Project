package com.recruitment.applicationservice.repository;

import com.recruitment.applicationservice.domain.ApplicationAssignment;
import com.recruitment.applicationservice.domain.AssignmentRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationAssignmentRepository extends JpaRepository<ApplicationAssignment, UUID> {

    @Query("""
            select a from ApplicationAssignment a
            join fetch a.application
            where a.application.id = :applicationId
            order by a.createdAt asc
            """)
    List<ApplicationAssignment> findByApplicationIdOrderByCreatedAtAsc(@Param("applicationId") UUID applicationId);

    @Query("""
            select a from ApplicationAssignment a
            join fetch a.application
            where a.userId = :userId
            order by a.createdAt desc
            """)
    List<ApplicationAssignment> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    Optional<ApplicationAssignment> findByIdAndApplicationId(UUID id, UUID applicationId);

    boolean existsByApplicationIdAndUserId(UUID applicationId, UUID userId);

    boolean existsByApplicationIdAndUserIdAndAssignmentRole(
            UUID applicationId,
            UUID userId,
            AssignmentRole assignmentRole
    );
}
