package com.rpe.orderservice.core.usecases;

import com.rpe.orderservice.core.domain.PaymentOption;
import com.rpe.orderservice.core.ports.inbound.ListPaymentOptionsUseCase;
import com.rpe.orderservice.core.ports.outbound.PaymentOptionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListPaymentOptionsUseCaseImpl implements ListPaymentOptionsUseCase {

    private final PaymentOptionRepositoryPort repositoryPort;

    @Override
    @Cacheable(value = "payment_methods")
    public List<PaymentOption> execute() {
        return repositoryPort.findAll();
    }
}