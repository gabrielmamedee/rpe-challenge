package com.rpe.orderservice.adapters.inbound.http.mapper;

import com.rpe.orderservice.adapters.inbound.http.dto.OrderRequest;
import com.rpe.orderservice.adapters.inbound.http.dto.OrderResponse;
import com.rpe.orderservice.core.domain.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
}