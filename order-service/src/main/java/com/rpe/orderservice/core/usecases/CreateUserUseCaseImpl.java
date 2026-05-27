package com.rpe.orderservice.core.usecases;

import com.rpe.orderservice.core.domain.Role;
import com.rpe.orderservice.core.domain.User;
import com.rpe.orderservice.core.domain.exceptions.DomainException;
import com.rpe.orderservice.core.ports.inbound.CreateUserUseCase;
import com.rpe.orderservice.core.ports.outbound.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateUserUseCaseImpl implements CreateUserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User execute(User user) {

        if (userRepositoryPort.existsByLogin(user.getLogin())) {
            throw new DomainException("Este login já está em uso.");
        }

        if (user.getPassword() == null || user.getPassword().length() < 6) {
            throw new DomainException("A senha deve conter pelo menos 6 caracteres.");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.ROLE_ORDER_CREATOR);

        return userRepositoryPort.save(user);
    }
}
