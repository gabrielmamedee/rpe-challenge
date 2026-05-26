package ports

import (
	"context"
	"payment-processor/internal/core/domain"
	"time"
)

type PaymentRepository interface {
	Save(ctx context.Context, payment *domain.Payment) error
	UpdateStatus(ctx context.Context, orderID string, status domain.PaymentStatus, paymentDate time.Time) error
}

type MessageQueue interface {
	PublishPixPending(ctx context.Context, orderID string) error
	PublishPixStatus(ctx context.Context, orderID string, status domain.PaymentStatus) error
}