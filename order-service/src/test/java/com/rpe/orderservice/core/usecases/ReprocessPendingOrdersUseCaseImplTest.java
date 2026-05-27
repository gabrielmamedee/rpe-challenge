package com.rpe.orderservice.core.usecases;

import com.rpe.orderservice.core.domain.Order;
import com.rpe.orderservice.core.domain.PaymentStatus;
import com.rpe.orderservice.core.ports.outbound.OrderRepositoryPort;
import com.rpe.orderservice.core.ports.outbound.PaymentIntegrationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReprocessPendingOrdersUseCaseImplTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @Mock
    private PaymentIntegrationPort paymentIntegrationPort;

    @InjectMocks
    private ReprocessPendingOrdersUseCaseImpl reprocessUseCase;

    private Order pendingOrder1;
    private Order pendingOrder2;

    @BeforeEach
    void setUp() {
        pendingOrder1 = new Order();
        pendingOrder1.setId(UUID.randomUUID());
        pendingOrder1.setStatus(PaymentStatus.PENDENTE_PAGAMENTO);

        pendingOrder2 = new Order();
        pendingOrder2.setId(UUID.randomUUID());
        pendingOrder2.setStatus(PaymentStatus.PENDENTE_PAGAMENTO);
    }

    @Test
    @DisplayName("Não deve fazer nada se não houver ordens pendentes no banco")
    void shouldDoNothingWhenThereAreNoPendingOrders() {
        // Arrange
        when(orderRepositoryPort.findByStatus(PaymentStatus.PENDENTE_PAGAMENTO))
                .thenReturn(Collections.emptyList());

        // Act
        reprocessUseCase.execute();

        // Assert
        verify(paymentIntegrationPort, never()).processPayment(any());
        verify(orderRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Deve reprocessar e salvar a ordem se o serviço em Go retornar um novo status (PAGO/RECUSADO)")
    void shouldReprocessAndSaveOrderSuccessfully() {
        // Arrange
        when(orderRepositoryPort.findByStatus(PaymentStatus.PENDENTE_PAGAMENTO))
                .thenReturn(List.of(pendingOrder1));

        Order processedOrder = new Order();
        processedOrder.setId(pendingOrder1.getId());
        processedOrder.setStatus(PaymentStatus.PAGO);

        when(paymentIntegrationPort.processPayment(pendingOrder1)).thenReturn(processedOrder);

        // Act
        reprocessUseCase.execute();

        // Assert
        verify(paymentIntegrationPort, times(1)).processPayment(pendingOrder1);
        verify(orderRepositoryPort, times(1)).save(processedOrder); // Garante que atualizou o banco
    }

    @Test
    @DisplayName("NÃO deve salvar no banco se o serviço em Go devolver a ordem ainda como PENDENTE_PAGAMENTO")
    void shouldNotSaveWhenStatusRemainsPending() {
        // Arrange
        when(orderRepositoryPort.findByStatus(PaymentStatus.PENDENTE_PAGAMENTO))
                .thenReturn(List.of(pendingOrder1));

        Order stillPendingOrder = new Order();
        stillPendingOrder.setId(pendingOrder1.getId());
        stillPendingOrder.setStatus(PaymentStatus.PENDENTE_PAGAMENTO); // Status continuou o mesmo

        when(paymentIntegrationPort.processPayment(pendingOrder1)).thenReturn(stillPendingOrder);

        // Act
        reprocessUseCase.execute();

        // Assert
        verify(paymentIntegrationPort, times(1)).processPayment(pendingOrder1);
        verify(orderRepositoryPort, never()).save(any()); // Não deve chamar o save atoa
    }

    @Test
    @DisplayName("Deve continuar o loop e processar a segunda ordem mesmo se a primeira lançar erro de conexão")
    void shouldContinueLoopWhenOneOrderFails() {
        // Arrange
        when(orderRepositoryPort.findByStatus(PaymentStatus.PENDENTE_PAGAMENTO))
                .thenReturn(Arrays.asList(pendingOrder1, pendingOrder2));

        // A primeira ordem vai dar erro (Simulando lentidão ou falha no Go)
        when(paymentIntegrationPort.processPayment(pendingOrder1))
                .thenThrow(new RuntimeException("Time out no serviço Go"));

        // A segunda ordem funciona
        Order processedOrder2 = new Order();
        processedOrder2.setId(pendingOrder2.getId());
        processedOrder2.setStatus(PaymentStatus.RECUSADO);

        when(paymentIntegrationPort.processPayment(pendingOrder2)).thenReturn(processedOrder2);

        // Act
        reprocessUseCase.execute();

        // Assert
        verify(paymentIntegrationPort, times(1)).processPayment(pendingOrder1);
        verify(paymentIntegrationPort, times(1)).processPayment(pendingOrder2);

        // Deve salvar apenas a segunda ordem, ignorando a falha da primeira
        verify(orderRepositoryPort, times(1)).save(processedOrder2);
        verify(orderRepositoryPort, never()).save(pendingOrder1);
    }
}