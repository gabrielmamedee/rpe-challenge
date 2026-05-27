package com.rpe.orderservice.adapters.outbound.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "payment_methods")
@Getter
@Setter
public class PaymentMethodDbEntity {
    @Id
    private UUID id;
    private String description;
}