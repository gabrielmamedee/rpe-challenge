package com.rpe.orderservice.adapters.inbound.sqs;

import com.rpe.orderservice.adapters.inbound.sqs.dto.PixNotificationMessage;
import com.rpe.orderservice.core.domain.PaymentStatus;
import com.rpe.orderservice.core.ports.inbound.UpdateOrderStatusUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PixNotificationListenerTest {

    @Mock
    private UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @InjectMocks
    private PixNotificationListener listener;

    @Test
    @DisplayName("Deve processar a mensagem SQS com sucesso e acionar o caso de uso")
    void shouldProcessMessageSuccessfully() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        PixNotificationMessage message = new PixNotificationMessage(orderId, "PAGO");

        // Act
        listener.listen(message);

        // Assert
        verify(updateOrderStatusUseCase, times(1)).execute(orderId, PaymentStatus.PAGO);
    }

    @Test
    @DisplayName("Deve repassar a exceção para que a AWS recoloque a mensagem na fila (DLQ)")
    void shouldThrowExceptionWhenUseCaseFails() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        PixNotificationMessage message = new PixNotificationMessage(orderId, "PAGO");

        doThrow(new RuntimeException("Database timeout"))
                .when(updateOrderStatusUseCase).execute(any(), any());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> listener.listen(message));
        verify(updateOrderStatusUseCase, times(1)).execute(orderId, PaymentStatus.PAGO);
    }
}