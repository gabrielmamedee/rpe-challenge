package usecases

import (
	"context"
	"testing"
	"time"

	"payment-processor/internal/core/domain"
)

type MockPaymentRepository struct {
	SaveCalled         bool
	UpdateStatusCalled bool
}

func (m *MockPaymentRepository) Save(ctx context.Context, payment *domain.Payment) error {
	m.SaveCalled = true
	return nil
}

func (m *MockPaymentRepository) UpdateStatus(ctx context.Context, orderID string, status domain.PaymentStatus, paymentDate time.Time) error {
	m.UpdateStatusCalled = true
	return nil
}

type MockMessageQueue struct {
	PublishPixPendingCalled bool
	PublishPixStatusCalled  bool
}

func (m *MockMessageQueue) PublishPixPending(ctx context.Context, orderID string) error {
	m.PublishPixPendingCalled = true
	return nil
}

func (m *MockMessageQueue) PublishPixStatus(ctx context.Context, orderID string, status domain.PaymentStatus) error {
	m.PublishPixStatusCalled = true
	return nil
}

func TestProcessPaymentUseCase_Pix(t *testing.T) {
	// Setup: Instanciamos os mocks e o Caso de Uso
	mockRepo := &MockPaymentRepository{}
	mockQueue := &MockMessageQueue{}
	uc := NewProcessPaymentUseCase(mockRepo, mockQueue)

	// Dado um pagamento via PIX
	payment := &domain.Payment{
		OrderID:       "123",
		PaymentMethod: domain.MethodPix,
	}

	// Executamos o caso de uso
	err := uc.Execute(context.Background(), payment)

	// Validações (Asserts)
	if err != nil {
		t.Fatalf("não era esperado um erro, mas recebeu: %v", err)
	}

	if payment.PaymentStatus != domain.StatusPendente {
		t.Errorf("esperado status %s, mas recebeu %s", domain.StatusPendente, payment.PaymentStatus)
	}

	if !mockRepo.SaveCalled {
		t.Errorf("esperado que o método Save do repositório fosse chamado")
	}

	if !mockQueue.PublishPixPendingCalled {
		t.Errorf("esperado que o método PublishPixPending da fila fosse chamado para pagamentos PIX")
	}

	if mockRepo.UpdateStatusCalled {
		t.Errorf("NÃO era esperado que UpdateStatus fosse chamado para o PIX neste momento")
	}
}

func TestProcessPaymentUseCase_Card(t *testing.T) {
	// Setup
	mockRepo := &MockPaymentRepository{}
	mockQueue := &MockMessageQueue{}
	uc := NewProcessPaymentUseCase(mockRepo, mockQueue)

	// Dado um pagamento via Cartão de Crédito
	payment := &domain.Payment{
		OrderID:       "456",
		PaymentMethod: domain.MethodCredito,
	}

	// Executamos
	err := uc.Execute(context.Background(), payment)

	// Validações
	if err != nil {
		t.Fatalf("não era esperado um erro, mas recebeu: %v", err)
	}

	if !mockRepo.SaveCalled {
		t.Errorf("esperado que o método Save fosse chamado")
	}

	if !mockRepo.UpdateStatusCalled {
		t.Errorf("esperado que o método UpdateStatus fosse chamado para pagamentos em Cartão")
	}

	if mockQueue.PublishPixPendingCalled {
		t.Errorf("NÃO era esperado o envio para a fila em pagamentos de Cartão")
	}

	if payment.PaymentStatus == domain.StatusPendente {
		t.Errorf("esperado que o status do cartão mudasse (Pago, Recusado, Cancelado), mas continuou Pendente")
	}
}

func TestGenerateRandomCardStatus_Distribution(t *testing.T) {
	
	iterations := 10000
	
	pagoCount := 0
	falhaCount := 0

	for i := 0; i < iterations; i++ {
		status := generateRandomCardStatus()
		if status == domain.StatusPago {
			pagoCount++
		} else if status == domain.StatusRecusado || status == domain.StatusCancelado {
			falhaCount++
		}
	}

	falhaPercentage := float64(falhaCount) / float64(iterations) * 100

	if falhaPercentage < 18.0 || falhaPercentage > 22.0 {
		t.Errorf("A distribuição de falhas quebrou a regra de 1 para 5 (20%%). Resultado atual: %.2f%%", falhaPercentage)
	}

	t.Logf("Distribuição em %d rodadas -> Pagos: %d | Falhas: %d (%.2f%%)", iterations, pagoCount, falhaCount, falhaPercentage)
}