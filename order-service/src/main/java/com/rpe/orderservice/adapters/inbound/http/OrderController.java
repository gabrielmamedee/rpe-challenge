package com.rpe.orderservice.adapters.inbound.http;

import com.rpe.orderservice.adapters.inbound.http.dto.OrderRequest;
import com.rpe.orderservice.adapters.inbound.http.dto.OrderResponse;
import com.rpe.orderservice.adapters.inbound.http.mapper.OrderMapper;
import com.rpe.orderservice.core.domain.Order;
import com.rpe.orderservice.core.domain.PaymentMethod;
import com.rpe.orderservice.core.domain.exceptions.DomainException;
import com.rpe.orderservice.core.ports.inbound.CreateOrderUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final OrderMapper orderMapper;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest dto) {

        Order order = orderMapper.toDomain(dto);

        try {
            order.setPaymentMethod(PaymentMethod.valueOf(dto.meio_pagamento().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new DomainException("Meio de pagamento inválido. Valores aceitos: PIX, CREDITO, DEBITO");
        }

        Order savedOrder = createOrderUseCase.execute(order);

        return ResponseEntity.status(HttpStatus.CREATED).body(orderMapper.toResponseDto(savedOrder));
    }
}