package com.rpe.orderservice.adapters.outbound.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserDbEntity, UUID> {
    Optional<UserDbEntity> findByLogin(String login);
    boolean existsByLogin(String login);
}
