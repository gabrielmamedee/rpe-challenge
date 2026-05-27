package com.rpe.orderservice.core.ports.outbound;

import com.rpe.orderservice.core.domain.Order;

public interface PaymentIntegrationPort {
    Order processPayment(Order order);
}