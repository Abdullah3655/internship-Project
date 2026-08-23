package com.recruitment.candidateservice.repository;

import com.recruitment.candidateservice.domain.Candidate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    @EntityGraph(attributePaths = "tags")
    Optional<Candidate> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNullAndIdNot(String email, UUID id);

    @EntityGraph(attributePaths = "tags")
    List<Candidate> findByDeletedAtIsNullOrderByCreatedAtDesc();

    @Query("""
            select distinct c from Candidate c
            join c.tags t
            where c.deletedAt is null and lower(t.name) = lower(:tag)
            order by c.createdAt desc
            """)
    List<Candidate> findByTagAndDeletedAtIsNull(@Param("tag") String tag);
}
