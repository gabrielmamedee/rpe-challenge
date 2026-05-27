package com.rpe.orderservice.core.usecases;

import com.rpe.orderservice.core.domain.Order;
import com.rpe.orderservice.core.domain.PaymentMethod;
import com.rpe.orderservice.core.domain.PaymentStatus;
import com.rpe.orderservice.core.domain.exceptions.DomainException;
import com.rpe.orderservice.core.ports.outbound.OrderRepositoryPort;
import com.rpe.orderservice.core.ports.outbound.PaymentIntegrationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseImplTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @Mock
    private PaymentIntegrationPort paymentIntegrationPort;

    @InjectMocks
    private CreateOrderUseCaseImpl createOrderUseCase;

    private Order validOrder;

    @BeforeEach
    void setUp() {
        validOrder = new Order();
        validOrder.setItemId(UUID.randomUUID());
        validOrder.setAmount(new BigDecimal("150.00"));
        validOrder.setPaymentMethod(PaymentMethod.PIX);
        validOrder.setBuyerName("João Silva");
        validOrder.setBuyerCpf("12345678901");
    }

    @Test
    @DisplayName("Deve lançar exceção se o valor for nulo ou menor/igual a zero")
    void shouldThrowExceptionWhenAmountIsInvalid() {
        validOrder.setAmount(BigDecimal.ZERO);
        assertThrows(DomainException.class, () -> createOrderUseCase.execute(validOrder));

        validOrder.setAmount(new BigDecimal("-10.00"));
        assertThrows(DomainException.class, () -> createOrderUseCase.execute(validOrder));

        validOrder.setAmount(null);
        assertThrows(DomainException.class, () -> createOrderUseCase.execute(validOrder));
    }

    @Test
    @DisplayName("Deve lançar exceção se o nome do comprador tiver 3 caracteres ou menos")
    void shouldThrowExceptionWhenBuyerNameIsTooShort() {
        validOrder.setBuyerName("Ana"); // 3 caracteres
        assertThrows(DomainException.class, () -> createOrderUseCase.execute(validOrder));

        validOrder.setBuyerName("   "); // Apenas espaços
        assertThrows(DomainException.class, () -> createOrderUseCase.execute(validOrder));
    }

    @Test
    @DisplayName("Deve lançar exceção se o CPF for inválido")
    void shouldThrowExceptionWhenCpfIsInvalid() {
        validOrder.setBuyerCpf("123"); // Faltam dígitos
        assertThrows(DomainException.class, () -> createOrderUseCase.execute(validOrder));

        validOrder.setBuyerCpf(null);
        assertThrows(DomainException.class, () -> createOrderUseCase.execute(validOrder));
    }

    @Test
    @DisplayName("Deve lançar exceção se o meio de pagamento for nulo")
    void shouldThrowExceptionWhenPaymentMethodIsNull() {
        validOrder.setPaymentMethod(null);
        assertThrows(DomainException.class, () -> createOrderUseCase.execute(validOrder));
    }

    @Test
    @DisplayName("Deve criar a ordem, salvar como pendente, chamar o Go e salvar o status final")
    void shouldCreateOrderAndProcessPaymentSuccessfully() {
        // Arrange
        when(orderRepositoryPort.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order processedOrder = new Order();
        processedOrder.setStatus(PaymentStatus.PAGO);
        when(paymentIntegrationPort.processPayment(any(Order.class))).thenReturn(processedOrder);

        // Act
        Order finalOrder = createOrderUseCase.execute(validOrder);

        // Assert
        assertNotNull(validOrder.getId()); // O ID deve ter sido gerado
        assertEquals(PaymentStatus.PAGO, finalOrder.getStatus());

        // Verifica se o save foi chamado 2 vezes (Uma para PENDENTE, outra para PAGO/RECUSADO)
        verify(orderRepositoryPort, times(2)).save(any(Order.class));
        verify(paymentIntegrationPort, times(1)).processPayment(any(Order.class));
    }
}