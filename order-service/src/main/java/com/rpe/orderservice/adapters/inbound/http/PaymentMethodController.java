package com.rpe.orderservice.adapters.inbound.http;

import com.rpe.orderservice.core.domain.PaymentOption;
import com.rpe.orderservice.core.ports.inbound.ListPaymentOptionsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final ListPaymentOptionsUseCase useCase;

    @GetMapping
    public ResponseEntity<List<PaymentOption>> list() {
        return ResponseEntity.ok(useCase.execute());
    }
}