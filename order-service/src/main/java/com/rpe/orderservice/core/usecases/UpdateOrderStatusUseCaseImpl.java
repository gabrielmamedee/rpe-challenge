package com.rpe.orderservice.core.usecases;

import com.rpe.orderservice.core.domain.Order;
import com.rpe.orderservice.core.domain.PaymentStatus;
import com.rpe.orderservice.core.ports.inbound.UpdateOrderStatusUseCase;
import com.rpe.orderservice.core.ports.outbound.OrderRepositoryPort;
import com.rpe.orderservice.core.ports.outbound.PaymentCallbackPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateOrderStatusUseCaseImpl implements UpdateOrderStatusUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final PaymentCallbackPort paymentCallbackPort;

    @Override
    public void execute(UUID orderId, PaymentStatus newStatus) {
        log.info("Processando atualização assíncrona para a Ordem: {} com status: {}", orderId, newStatus);

        Order order = orderRepositoryPort.findById(orderId).orElse(null);
        if (order == null) {
            log.error("Ordem {} não encontrada para atualização via SQS.", orderId);
            return;
        }

        order.updateStatus(newStatus, LocalDateTime.now());

        orderRepositoryPort.save(order);
        log.info("Ordem {} atualizada via SQS com sucesso!", orderId);

        try {
            paymentCallbackPort.notifyPaymentStatus(order.getId(), order.getStatus(), order.getPaymentDate());
            log.info("Callback enviado ao payment-processor para a ordem {}", orderId);
        } catch (Exception e) {
            log.error("Erro ao enviar callback para o payment-processor: {}", e.getMessage());
        }
    }
}