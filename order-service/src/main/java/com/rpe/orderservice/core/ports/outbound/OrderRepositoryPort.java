package com.rpe.orderservice.core.ports.outbound;

import com.rpe.orderservice.core.domain.Order;
import com.rpe.orderservice.core.domain.PaymentStatus;

import java.util.List;
import java.util.UUID;

public interface OrderRepositoryPort {
    Order save(Order order);
    List<Order> findByStatus(PaymentStatus status);
}