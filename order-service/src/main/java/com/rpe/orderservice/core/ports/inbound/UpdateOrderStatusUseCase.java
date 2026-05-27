package com.rpe.orderservice.core.ports.inbound;

import com.rpe.orderservice.core.domain.PaymentStatus;
import java.util.UUID;

public interface UpdateOrderStatusUseCase {
    void execute(UUID orderId, PaymentStatus newStatus);
}