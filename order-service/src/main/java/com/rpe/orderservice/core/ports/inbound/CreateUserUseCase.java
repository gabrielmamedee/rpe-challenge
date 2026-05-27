package com.rpe.orderservice.core.ports.inbound;

import com.rpe.orderservice.core.domain.User;

public interface CreateUserUseCase {
    User execute(User user);
}
