package com.rpe.orderservice.core.ports.outbound;

import com.rpe.orderservice.core.domain.PaymentStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public interface PaymentCallbackPort {
    void notifyPaymentStatus(UUID orderId, PaymentStatus status, LocalDateTime paymentDate);
}