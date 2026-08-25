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
            select c from Candidate c
            where c.deletedAt is null
              and (
                select count(distinct lower(t.name))
                from c.tags t
                where lower(t.name) in :tags
              ) = :tagCount
            order by c.createdAt desc
            """)
    List<Candidate> findByAllTagsAndDeletedAtIsNull(
            @Param("tags") List<String> tags,
            @Param("tagCount") long tagCount
    );
}
