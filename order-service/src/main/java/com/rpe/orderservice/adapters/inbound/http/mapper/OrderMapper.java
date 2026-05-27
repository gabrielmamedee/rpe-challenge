package com.rpe.orderservice.adapters.inbound.http.mapper;

import com.rpe.orderservice.adapters.inbound.http.dto.OrderRequest;
import com.rpe.orderservice.adapters.inbound.http.dto.OrderResponse;
import com.rpe.orderservice.adapters.inbound.http.dto.OrderSummaryResponse;
import com.rpe.orderservice.core.domain.Order;
import com.rpe.orderservice.core.domain.PaymentMethod;
import com.rpe.orderservice.core.domain.exceptions.DomainException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "paymentDate", ignore = true)
    @Mapping(target = "itemId", source = "id_item")
    @Mapping(target = "amount", source = "valor")
    @Mapping(target = "paymentMethod", source = "meio_pagamento")
    @Mapping(target = "buyerName", source = "nome_comprador")
    @Mapping(target = "buyerCpf", source = "cpf_comprador")
    Order toDomain(OrderRequest dto);

    @Mapping(target = "id_ordem", source = "id")
    @Mapping(target = "data_criacao", source = "createdAt")
    @Mapping(target = "data_pagamento", source = "paymentDate")
    OrderResponse toResponseDto(Order order);

    default PaymentMethod mapPaymentMethod(String meioPagamento) {
        if (meioPagamento == null || meioPagamento.isBlank()) {
            return null;
        }
        try {
            return PaymentMethod.valueOf(meioPagamento.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("Meio de pagamento inválido. Valores aceitos: PIX, CREDITO, DEBITO");
        }
    }

    default List<OrderSummaryResponse> toSummaryResponseList(List<Order> orders) {
        if (orders == null) {
            return java.util.List.of();
        }
        return orders.stream()
                .map(order -> new OrderSummaryResponse(
                        order.getId(),
                        order.getBuyerName(),
                        order.getStatus() != null ? order.getStatus().name() : null
                ))
                .toList();
    }
}
