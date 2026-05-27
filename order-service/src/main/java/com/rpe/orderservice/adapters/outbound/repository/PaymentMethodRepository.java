package com.rpe.orderservice.adapters.outbound.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethodDbEntity, UUID> {
}
