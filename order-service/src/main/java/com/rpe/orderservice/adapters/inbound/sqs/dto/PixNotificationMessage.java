package com.rpe.orderservice.adapters.inbound.sqs.dto;

import java.util.UUID;

public record PixNotificationMessage(
        UUID id_ordem,
        String status_pagamento
) {
}