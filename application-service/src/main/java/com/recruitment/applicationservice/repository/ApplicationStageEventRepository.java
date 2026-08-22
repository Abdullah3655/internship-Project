package com.recruitment.applicationservice.repository;

import com.recruitment.applicationservice.domain.ApplicationStageEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationStageEventRepository extends JpaRepository<ApplicationStageEvent, UUID> {

    List<ApplicationStageEvent> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}
