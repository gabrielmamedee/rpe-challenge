package com.rpe.orderservice.adapters.outbound.http.dto;

import java.math.BigDecimal;

public record PaymentIntegrationRequest(
        String id_ordem,
        String id_item,
        BigDecimal valor,
        String meio_pagamento,
        String nome_comprador,
        String cpf_comprador
) {
}