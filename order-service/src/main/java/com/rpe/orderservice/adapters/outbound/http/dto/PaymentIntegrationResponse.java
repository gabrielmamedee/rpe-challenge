package com.rpe.orderservice.adapters.outbound.http.dto;

import java.time.LocalDateTime;

public record PaymentIntegrationResponse(
        String id_ordem,
        String status_pagamento,
        LocalDateTime data_pagamento
) {
}
