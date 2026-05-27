package com.rpe.orderservice.core.ports.inbound;

import com.rpe.orderservice.core.domain.Order;

public interface CreateOrderUseCase {
    Order execute(Order order);
}