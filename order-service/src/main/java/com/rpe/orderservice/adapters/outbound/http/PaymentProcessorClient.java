package com.rpe.orderservice.adapters.outbound.http;

import com.rpe.orderservice.adapters.outbound.http.dto.PaymentIntegrationRequest;
import com.rpe.orderservice.adapters.outbound.http.dto.PaymentIntegrationResponse;
import com.rpe.orderservice.adapters.outbound.http.dto.PaymentStatusUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-processor", url = "${payment.processor.url:http://localhost:8081}")
public interface PaymentProcessorClient {

    @PostMapping("/api/v1/payments")
    PaymentIntegrationResponse processPayment(@RequestBody PaymentIntegrationRequest request);

    @PatchMapping("/api/v1/payments/status")
    void updatePaymentStatus(PaymentStatusUpdateRequest request);

}