package com.rpe.orderservice.adapters.outbound.http.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentStatusUpdateRequest(
        UUID id_ordem,
        String status_pagamento,
        OffsetDateTime data_pagamento
) {}
