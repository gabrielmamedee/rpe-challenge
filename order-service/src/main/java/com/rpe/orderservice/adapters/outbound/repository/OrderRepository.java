package com.rpe.orderservice.adapters.outbound.repository;

import com.rpe.orderservice.core.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderDbEntity, UUID> {
    List<OrderDbEntity> findByStatus(PaymentStatus status);
    List<OrderDbEntity> findByBuyerCpf(String buyerCpf);
}