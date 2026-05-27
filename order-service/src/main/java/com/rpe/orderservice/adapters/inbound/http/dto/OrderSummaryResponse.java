package com.rpe.orderservice.adapters.inbound.http.dto;

import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String nome_comprador,
        String status
) {}