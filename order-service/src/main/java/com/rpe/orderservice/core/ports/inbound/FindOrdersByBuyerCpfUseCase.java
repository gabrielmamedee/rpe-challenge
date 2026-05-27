package com.rpe.orderservice.core.ports.inbound;

import com.rpe.orderservice.core.domain.Order;
import java.util.List;

public interface FindOrdersByBuyerCpfUseCase {
    List<Order> execute(String cpf);
}
