package com.rpe.orderservice.core.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class Order {

    private UUID id;
    private UUID itemId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String buyerName;
    private String buyerCpf;

    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paymentDate;

    public Order() {
        this.status = PaymentStatus.PENDENTE_PAGAMENTO;
        this.createdAt = LocalDateTime.now();
    }

    public void updateStatus(PaymentStatus newStatus, LocalDateTime paymentDate) {
        this.status = newStatus;
        if (newStatus == PaymentStatus.PAGO || newStatus == PaymentStatus.CANCELADO || newStatus == PaymentStatus.RECUSADO || newStatus == PaymentStatus.REPROVADO) {
            this.paymentDate = paymentDate;
        }
    }
}
