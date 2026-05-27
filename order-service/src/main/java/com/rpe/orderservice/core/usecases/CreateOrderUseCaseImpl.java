package com.rpe.orderservice.core.usecases;

import com.rpe.orderservice.core.domain.Order;
import com.rpe.orderservice.core.domain.exceptions.DomainException;
import com.rpe.orderservice.core.ports.inbound.CreateOrderUseCase;
import com.rpe.orderservice.core.ports.outbound.OrderRepositoryPort;
import com.rpe.orderservice.core.ports.outbound.PaymentIntegrationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateOrderUseCaseImpl implements CreateOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final PaymentIntegrationPort paymentIntegrationPort;

    @Override
    public Order execute(Order order) {

        if (order.getAmount() == null || order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("O valor da ordem não pode ser nulo, zero ou menor que zero.");
        }
        if (order.getBuyerName() == null || order.getBuyerName().trim().length() <= 3) {
            throw new DomainException("O nome do comprador deve conter mais de 3 caracteres.");
        }
        if (order.getBuyerCpf() == null || !isValidCpf(order.getBuyerCpf())) {
            throw new DomainException("CPF inválido ou não preenchido.");
        }
        if (order.getPaymentMethod() == null) {
            throw new DomainException("O meio de pagamento é obrigatório (PIX, CREDITO, DEBITO).");
        }

        order.setId(UUID.randomUUID());
        order = orderRepositoryPort.save(order);
        Order processedOrder = paymentIntegrationPort.processPayment(order);

        return orderRepositoryPort.save(processedOrder);
    }

    private boolean isValidCpf(String cpf) {
        String cleanCpf = cpf.replaceAll("\\D", "");
        return cleanCpf.length() == 11;
    }
}