package com.rpe.orderservice.core.usecases;

import com.rpe.orderservice.core.domain.Order;
import com.rpe.orderservice.core.domain.PaymentStatus;
import com.rpe.orderservice.core.ports.outbound.OrderRepositoryPort;
import com.rpe.orderservice.core.ports.outbound.PaymentCallbackPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateOrderStatusUseCaseImplTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @Mock
    private PaymentCallbackPort paymentCallbackPort;

    @InjectMocks
    private UpdateOrderStatusUseCaseImpl updateOrderStatusUseCase;

    private Order existingOrder;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setStatus(PaymentStatus.PENDENTE_PAGAMENTO);
    }

    @Test
    @DisplayName("Deve atualizar o status, salvar a ordem e disparar o callback quando a ordem existir")
    void shouldUpdateSaveAndNotifyWhenOrderExists() {
        // Arrange
        when(orderRepositoryPort.findById(orderId)).thenReturn(Optional.of(existingOrder));

        // Act
        updateOrderStatusUseCase.execute(orderId, PaymentStatus.PAGO);

        // Assert
        assertEquals(PaymentStatus.PAGO, existingOrder.getStatus());
        assertNotNull(existingOrder.getPaymentDate()); // Garante que o updateStatus preencheu a data
        verify(orderRepositoryPort, times(1)).save(existingOrder);

        // 2. Verificamos se o callback para o serviço em Go foi chamado corretamente
        verify(paymentCallbackPort, times(1))
                .notifyPaymentStatus(eq(orderId), eq(PaymentStatus.PAGO), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Não deve fazer nada se a ordem não for encontrada")
    void shouldDoNothingWhenOrderDoesNotExist() {
        // Arrange
        when(orderRepositoryPort.findById(orderId)).thenReturn(Optional.empty());

        // Act
        updateOrderStatusUseCase.execute(orderId, PaymentStatus.PAGO);

        // Assert
        verify(orderRepositoryPort, never()).save(any());

        // 3. Garante que não envia notificação fantasma
        verify(paymentCallbackPort, never()).notifyPaymentStatus(any(), any(), any());
    }
}