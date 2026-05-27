package com.rpe.orderservice.core.ports.inbound;

import com.rpe.orderservice.core.domain.PaymentOption;
import java.util.List;

public interface ListPaymentOptionsUseCase {
    List<PaymentOption> execute();
}