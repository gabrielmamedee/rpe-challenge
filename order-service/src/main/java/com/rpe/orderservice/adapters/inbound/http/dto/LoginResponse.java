package com.rpe.orderservice.adapters.inbound.http.dto;

public record LoginResponse(
        String token,
        String type
) {
}
