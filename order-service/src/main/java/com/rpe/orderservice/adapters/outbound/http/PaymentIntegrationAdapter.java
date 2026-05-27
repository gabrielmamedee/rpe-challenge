package com.rpe.orderservice.adapters.outbound.http;

import com.rpe.orderservice.adapters.outbound.http.dto.PaymentIntegrationRequest;
import com.rpe.orderservice.core.domain.Order;
import com.rpe.orderservice.core.domain.PaymentStatus;
import com.rpe.orderservice.core.domain.exceptions.DomainException;
import com.rpe.orderservice.core.ports.outbound.PaymentIntegrationPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentIntegrationAdapter implements PaymentIntegrationPort {

    private final PaymentProcessorClient client;

    @Override
    @CircuitBreaker(name = "paymentProcessor", fallbackMethod = "fallbackProcessPayment")
    public Order processPayment(Order order) {
        log.info("Iniciando comunicação síncrona com o serviço em Go para a ordem: {}", order.getId());

        var request = new PaymentIntegrationRequest(
                order.getId().toString(),
                order.getItemId().toString(),
                order.getAmount(),
                order.getPaymentMethod().name(),
                order.getBuyerName(),
                order.getBuyerCpf()
        );

        var response = client.processPayment(request);

        order.updateStatus(PaymentStatus.fromString(response.status_pagamento()), response.data_pagamento());
        log.info("Sucesso! Ordem {} atualizada para status: {}", order.getId(), order.getStatus());

        return order;
    }

    public Order fallbackProcessPayment(Order order, Throwable t) {
        log.warn("Serviço de pagamentos (Go) indisponível. Ordem {} mantida como PENDENTE. Motivo: {}", order.getId(), t.getMessage());
        order.setStatus(com.rpe.orderservice.core.domain.PaymentStatus.PENDENTE_PAGAMENTO);
        return order;
    }
}