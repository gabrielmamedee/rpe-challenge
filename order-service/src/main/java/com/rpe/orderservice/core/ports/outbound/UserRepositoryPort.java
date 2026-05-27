package com.rpe.orderservice.core.ports.outbound;

import com.rpe.orderservice.core.domain.User;

import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findByLogin(String login);
    boolean existsByLogin(String login);
}
