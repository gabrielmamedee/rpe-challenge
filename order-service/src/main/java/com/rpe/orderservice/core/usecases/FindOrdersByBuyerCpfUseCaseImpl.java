package com.rpe.orderservice.core.usecases;

import com.rpe.orderservice.core.domain.Order;
import com.rpe.orderservice.core.domain.exceptions.DomainException;
import com.rpe.orderservice.core.ports.inbound.FindOrdersByBuyerCpfUseCase;
import com.rpe.orderservice.core.ports.outbound.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindOrdersByBuyerCpfUseCaseImpl implements FindOrdersByBuyerCpfUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    @Override
    public List<Order> execute(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            throw new DomainException("O CPF do comprador é obrigatório para realizar a consulta.");
        }

        return orderRepositoryPort.findByBuyerCpf(cpf.trim());
    }
}