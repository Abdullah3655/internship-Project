package com.recruitment.authservice.repository;

import com.recruitment.authservice.domain.User;
import com.recruitment.authservice.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    List<User> findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc();

    List<User> findByRoleAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(UserRole role);
}
