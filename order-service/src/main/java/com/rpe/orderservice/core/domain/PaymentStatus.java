package com.rpe.orderservice.core.domain;

public enum PaymentStatus {
    PENDENTE_PAGAMENTO,
    PAGO,
    CANCELADO,
    RECUSADO,
    REPROVADO;

    public static PaymentStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PENDENTE_PAGAMENTO;
        }

        String normalized = value.trim()
                .toUpperCase()
                .replace(" ", "_")
                .replace("-", "_");

        try {
            return PaymentStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return PENDENTE_PAGAMENTO;
        }
    }
}
