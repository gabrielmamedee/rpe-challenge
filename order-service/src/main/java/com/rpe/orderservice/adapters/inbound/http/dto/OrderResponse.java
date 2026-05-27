package com.rpe.orderservice.adapters.inbound.http.dto;

import com.rpe.orderservice.core.domain.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id_ordem,
        PaymentStatus status,
        LocalDateTime data_criacao,
        LocalDateTime data_pagamento
) {
}