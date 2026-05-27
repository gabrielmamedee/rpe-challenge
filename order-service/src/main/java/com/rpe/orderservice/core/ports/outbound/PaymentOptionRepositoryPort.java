package com.rpe.orderservice.core.ports.outbound;

import com.rpe.orderservice.core.domain.PaymentOption;
import java.util.List;

public interface PaymentOptionRepositoryPort {
    List<PaymentOption> findAll();
}