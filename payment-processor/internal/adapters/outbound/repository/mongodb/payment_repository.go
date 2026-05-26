package mongodb

import (
	"context"
	"time"

	"payment-processor/internal/core/domain"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
)

type PaymentRepository struct {
	collection *mongo.Collection
}
func NewPaymentRepository(db *mongo.Database) *PaymentRepository {
	return &PaymentRepository{
		collection: db.Collection("payments"),
	}
}

func (r *PaymentRepository) Save(ctx context.Context, payment *domain.Payment) error {
	_, err := r.collection.InsertOne(ctx, payment)
	return err 
}

func (r *PaymentRepository) UpdateStatus(ctx context.Context, orderID string, status domain.PaymentStatus, paymentDate time.Time) error {
	filter := bson.M{"id_ordem": orderID}
	update := bson.M{
		"$set": bson.M{
			"status_pagamento": status,
			"data_pagamento":   paymentDate,
		},
	}

	_, err := r.collection.UpdateOne(ctx, filter, update)
	return err
}