package com.rpe.orderservice.adapters.inbound.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderRequest(

        @NotNull(message = "O id do item é obrigatório")
        UUID id_item,

        @NotNull(message = "O valor é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        BigDecimal valor,

        @NotBlank(message = "O meio de pagamento é obrigatório")
        String meio_pagamento,

        @NotBlank(message = "O nome do comprador é obrigatório")
        @Size(min = 4, message = "O nome do comprador deve ter mais de 3 caracteres")
        String nome_comprador,

        @NotBlank(message = "O CPF do comprador é obrigatório")
        String cpf_comprador
) {
}