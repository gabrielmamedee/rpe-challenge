package com.rpe.orderservice.adapters.outbound.repository;

import com.rpe.orderservice.core.domain.PaymentOption;
import com.rpe.orderservice.core.ports.outbound.PaymentOptionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PaymentOptionRepositoryAdapter implements PaymentOptionRepositoryPort {

    private final PaymentMethodRepository repository;

    @Override
    public List<PaymentOption> findAll() {
        return repository.findAll().stream()
                .map(entity -> new PaymentOption(entity.getId(), entity.getDescription()))
                .collect(Collectors.toList());
    }
}