package com.rpe.orderservice.adapters.outbound.repository;

import com.rpe.orderservice.core.domain.PaymentMethod;
import com.rpe.orderservice.core.domain.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class OrderDbEntity {
    @Id
    private UUID id;
    private UUID itemId;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String buyerName;
    private String buyerCpf;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime paymentDate;
}