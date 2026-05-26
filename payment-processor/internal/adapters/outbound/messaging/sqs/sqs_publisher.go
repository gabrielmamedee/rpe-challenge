package sqs

import (
	"context"
	"encoding/json"
	"fmt"

	"payment-processor/internal/core/domain"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
)

type SQSPublisher struct {
	client            *sqs.Client
	pixPendingQueue   string
	pixStatusQueue    string
}

func NewSQSPublisher(client *sqs.Client, pendingQueueURL, statusQueueURL string) *SQSPublisher {
	return &SQSPublisher{
		client:          client,
		pixPendingQueue: pendingQueueURL,
		pixStatusQueue:  statusQueueURL,
	}
}

func (p *SQSPublisher) PublishPixPending(ctx context.Context, orderID string) error {
	payload := map[string]string{
		"id_ordem": orderID,
	}
	
	
	bodyBytes, err := json.Marshal(payload)
	if err != nil {
		return fmt.Errorf("erro ao converter payload para json: %w", err)
	}
	bodyString := string(bodyBytes)

	groupID := "pix-processing-group"

	input := &sqs.SendMessageInput{
		QueueUrl:               aws.String(p.pixPendingQueue),
		MessageBody:            aws.String(bodyString),
		MessageGroupId:         aws.String(groupID),
		MessageDeduplicationId: aws.String(orderID), 
	}

	_, err = p.client.SendMessage(ctx, input)
	if err != nil {
		return fmt.Errorf("erro ao enviar mensagem para a fila pendente: %w", err)
	}

	return nil
}

func (p *SQSPublisher) PublishPixStatus(ctx context.Context, orderID string, status domain.PaymentStatus) error {
	payload := map[string]string{
		"id_ordem":         orderID,
		"status_pagamento": string(status),
	}
	
	bodyBytes, err := json.Marshal(payload)
	if err != nil {
		return fmt.Errorf("erro ao converter payload para json: %w", err)
	}

	input := &sqs.SendMessageInput{
		QueueUrl:    aws.String(p.pixStatusQueue),
		MessageBody: aws.String(string(bodyBytes)),
	}

	_, err = p.client.SendMessage(ctx, input)
	if err != nil {
		return fmt.Errorf("erro ao enviar mensagem para a fila de status: %w", err)
	}

	return nil
}