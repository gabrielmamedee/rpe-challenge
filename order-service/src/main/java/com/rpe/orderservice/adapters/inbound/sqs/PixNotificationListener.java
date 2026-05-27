package com.rpe.orderservice.adapters.inbound.sqs;

import com.rpe.orderservice.adapters.inbound.sqs.dto.PixNotificationMessage;
import com.rpe.orderservice.core.domain.PaymentStatus;
import com.rpe.orderservice.core.ports.inbound.UpdateOrderStatusUseCase;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PixNotificationListener {

    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @SqsListener("${aws.sqs.queue.pix-notifications}")
    public void listen(PixNotificationMessage message) {
        log.info("Mensagem SQS recebida para a ordem: {}", message.id_ordem());

        try {
            PaymentStatus status = PaymentStatus.fromString(message.status_pagamento());

            updateOrderStatusUseCase.execute(message.id_ordem(), status);

        } catch (Exception e) {
            log.error("Erro ao processar mensagem do SQS: {}", e.getMessage());
            throw e;
        }
    }
}