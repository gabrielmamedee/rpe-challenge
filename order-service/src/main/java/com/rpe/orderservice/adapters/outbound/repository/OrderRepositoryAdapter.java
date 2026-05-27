package com.rpe.orderservice.adapters.outbound.repository;

import com.rpe.orderservice.core.domain.Order;
import com.rpe.orderservice.core.domain.PaymentStatus;
import com.rpe.orderservice.core.ports.outbound.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final OrderRepository repository;

    @Override
    public Order save(Order order) {
        var entity = new OrderDbEntity();
        entity.setId(order.getId());
        entity.setItemId(order.getItemId());
        entity.setAmount(order.getAmount());
        entity.setPaymentMethod(order.getPaymentMethod());
        entity.setBuyerName(order.getBuyerName());
        entity.setBuyerCpf(order.getBuyerCpf());
        entity.setStatus(order.getStatus());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setPaymentDate(order.getPaymentDate());

        var savedEntity = repository.save(entity);

        order.setId(savedEntity.getId());
        return order;
    }

    @Override
    public List<Order> findByStatus(PaymentStatus status) {
        return repository.findByStatus(status).stream()
                .map(entity -> {
                    Order order = new Order();
                    order.setId(entity.getId());
                    order.setItemId(entity.getItemId());
                    order.setAmount(entity.getAmount());
                    order.setPaymentMethod(entity.getPaymentMethod());
                    order.setBuyerName(entity.getBuyerName());
                    order.setBuyerCpf(entity.getBuyerCpf());
                    order.setStatus(entity.getStatus());
                    order.setCreatedAt(entity.getCreatedAt());
                    order.setPaymentDate(entity.getPaymentDate());
                    return order;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return repository.findById(id).map(entity -> {
            Order order = new Order();
            order.setId(entity.getId());
            order.setItemId(entity.getItemId());
            order.setAmount(entity.getAmount());
            order.setPaymentMethod(entity.getPaymentMethod());
            order.setBuyerName(entity.getBuyerName());
            order.setBuyerCpf(entity.getBuyerCpf());
            order.setStatus(entity.getStatus());
            order.setCreatedAt(entity.getCreatedAt());
            order.setPaymentDate(entity.getPaymentDate());
            return order;
        });
    }

    @Override
    public List<Order> findByBuyerCpf(String buyerCpf) {
        return repository.findByBuyerCpf(buyerCpf).stream()
                .map(entity -> {
                    Order order = new Order();
                    order.setId(entity.getId());
                    order.setItemId(entity.getItemId());
                    order.setAmount(entity.getAmount());
                    order.setPaymentMethod(entity.getPaymentMethod());
                    order.setBuyerName(entity.getBuyerName());
                    order.setBuyerCpf(entity.getBuyerCpf());
                    order.setStatus(entity.getStatus());
                    order.setCreatedAt(entity.getCreatedAt());
                    order.setPaymentDate(entity.getPaymentDate());
                    return order;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}