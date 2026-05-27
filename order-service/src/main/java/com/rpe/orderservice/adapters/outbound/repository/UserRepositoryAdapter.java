package com.rpe.orderservice.adapters.outbound.repository;

import com.rpe.orderservice.core.domain.User;
import com.rpe.orderservice.core.ports.outbound.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserRepository userRepository;

    @Override
    public User save(User user) {
        UserDbEntity entity = new UserDbEntity();
        entity.setLogin(user.getLogin());
        entity.setPassword(user.getPassword());
        entity.setRole(user.getRole());

        UserDbEntity savedEntity = userRepository.save(entity);

        user.setId(savedEntity.getId());
        return user;
    }

    @Override
    public Optional<User> findByLogin(String login) {
        return userRepository.findByLogin(login)
                .map(entity -> {
                    User user = new User();
                    user.setId(entity.getId());
                    user.setLogin(entity.getLogin());
                    user.setPassword(entity.getPassword());
                    user.setRole(entity.getRole());
                    return user;
                });
    }

    @Override
    public boolean existsByLogin(String login) {
        return userRepository.existsByLogin(login);
    }
}
