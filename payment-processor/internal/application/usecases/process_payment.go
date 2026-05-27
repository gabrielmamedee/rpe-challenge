package usecases

import (
	"context"
	"fmt"
	"math/rand"
	"time"

	"payment-processor/internal/core/domain"
	"payment-processor/internal/core/ports"
)

type ProcessPaymentUseCase struct {
	repo  ports.PaymentRepository
	queue ports.MessageQueue
}

func NewProcessPaymentUseCase(repo ports.PaymentRepository, queue ports.MessageQueue) *ProcessPaymentUseCase {
	return &ProcessPaymentUseCase{
		repo:  repo,
		queue: queue,
	}
}

func (uc *ProcessPaymentUseCase) Execute(ctx context.Context, payment *domain.Payment) error {
	
	payment.PaymentStatus = domain.StatusPendente
	payment.ProcessingDate = time.Now()

	err := uc.repo.Save(ctx, payment)
	if err != nil {
		return fmt.Errorf("erro ao salvar pagamento inicial: %w", err)
	}

	if payment.PaymentMethod == domain.MethodPix {
		return uc.processPix(ctx, payment)
	}

	return uc.processCard(ctx, payment)
}

func (uc *ProcessPaymentUseCase) processPix(ctx context.Context, payment *domain.Payment) error {

	err := uc.queue.PublishPixPending(ctx, payment.OrderID)
	if err != nil {
		return fmt.Errorf("erro ao enviar pix para a fila: %w", err)
	}
	return nil
}


func (uc *ProcessPaymentUseCase) processCard(ctx context.Context, payment *domain.Payment) error {
	
	finalStatus := generateRandomStatus(domain.StatusRecusado)
	now := time.Now()


	err := uc.repo.UpdateStatus(ctx, payment.OrderID, finalStatus, now)
	if err != nil {
		return fmt.Errorf("erro ao atualizar status do cartao: %w", err)
	}

	payment.PaymentStatus = finalStatus
	payment.PaymentDate = &now

	return nil
}

func (uc *ProcessPaymentUseCase) UpdateStatus(ctx context.Context, orderID string, status domain.PaymentStatus, paymentDate time.Time) error {
	return uc.repo.UpdateStatus(ctx, orderID, status, paymentDate)
}

func (uc *ProcessPaymentUseCase) CompletePixProcess(ctx context.Context, orderID string) error {
	
	finalStatus := generateRandomStatus(domain.StatusReprovado)

	if err := uc.repo.UpdateStatus(ctx, orderID, finalStatus, time.Now()); err != nil {
		return err
	}

	return uc.queue.PublishPixStatus(ctx, orderID, finalStatus)
}

func generateRandomStatus(specificFailureStatus domain.PaymentStatus) domain.PaymentStatus {
	chance := rand.Intn(5)

	if chance == 0 {
		if rand.Intn(2) == 0 {
			return specificFailureStatus
		}
		return domain.StatusCancelado
	}

	return domain.StatusPago
}