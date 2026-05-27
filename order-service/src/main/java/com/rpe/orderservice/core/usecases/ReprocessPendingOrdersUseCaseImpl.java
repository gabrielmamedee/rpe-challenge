package com.rpe.orderservice.core.usecases;

import com.rpe.orderservice.core.domain.Order;
import com.rpe.orderservice.core.domain.PaymentStatus;
import com.rpe.orderservice.core.ports.inbound.ReprocessPendingOrdersUseCase;
import com.rpe.orderservice.core.ports.outbound.OrderRepositoryPort;
import com.rpe.orderservice.core.ports.outbound.PaymentIntegrationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReprocessPendingOrdersUseCaseImpl implements ReprocessPendingOrdersUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final PaymentIntegrationPort paymentIntegrationPort;

    @Override
    public void execute() {
        List<Order> pendingOrders = orderRepositoryPort.findByStatus(PaymentStatus.PENDENTE_PAGAMENTO);

        if (pendingOrders.isEmpty()) {
            return;
        }

        log.info("Loop de Resiliência: Encontradas {} ordens pendentes para reprocessamento.", pendingOrders.size());

        for (Order order : pendingOrders) {
            try {
                Order processedOrder = paymentIntegrationPort.processPayment(order);

                if (processedOrder.getStatus() != PaymentStatus.PENDENTE_PAGAMENTO) {
                    orderRepositoryPort.save(processedOrder);
                    log.info("Ordem {} reprocessada com sucesso! Novo status: {}", order.getId(), processedOrder.getStatus());
                }
            } catch (Exception e) {
                log.warn("Ordem {} continuará pendente. O serviço em Go ainda está indisponível.", order.getId());
            }
        }
    }
}