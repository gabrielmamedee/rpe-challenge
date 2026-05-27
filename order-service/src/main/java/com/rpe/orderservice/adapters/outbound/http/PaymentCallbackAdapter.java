package com.rpe.orderservice.adapters.outbound.http;

import com.rpe.orderservice.adapters.outbound.http.dto.PaymentStatusUpdateRequest;
import com.rpe.orderservice.core.domain.PaymentStatus;
import com.rpe.orderservice.core.ports.outbound.PaymentCallbackPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentCallbackAdapter implements PaymentCallbackPort {

    private final PaymentProcessorClient paymentProcessorClient;

    @Override
    public void notifyPaymentStatus(UUID orderId, PaymentStatus status, LocalDateTime paymentDate) {

        OffsetDateTime offsetDate = null;
        if (paymentDate != null) {
            offsetDate = paymentDate.atZone(ZoneId.systemDefault()).toOffsetDateTime();
        }

        PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest(
                orderId,
                status.name(),
                offsetDate
        );

        paymentProcessorClient.updatePaymentStatus(request);
    }
}