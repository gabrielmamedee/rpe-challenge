package com.rpe.orderservice.adapters.inbound.http.dto;

import com.rpe.orderservice.core.domain.Role;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String login,
        Role role
) {
}
