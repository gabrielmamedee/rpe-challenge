package sqs

import (
	"context"
	"encoding/json"
	"log"
	"time"

	"payment-processor/internal/application/usecases"
	"payment-processor/internal/core/domain"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
)

type SQSConsumer struct {
	client          *sqs.Client
	pendingQueueURL string
	useCase         *usecases.ProcessPaymentUseCase
}

func NewSQSConsumer(client *sqs.Client, pendingQueueURL string, uc *usecases.ProcessPaymentUseCase) *SQSConsumer {
	return &SQSConsumer{
		client:          client,
		pendingQueueURL: pendingQueueURL,
		useCase:         uc,
	}
}

func (c *SQSConsumer) Start(ctx context.Context) {
	log.Println("Iniciando Worker SQS para ler a fila PIX Pendente...")

	for {
		select {
		case <-ctx.Done():
			log.Println("Worker SQS finalizado.")
			return
		default:
			c.pollMessages(ctx)
			time.Sleep(2 * time.Second)
		}
	}
}

func (c *SQSConsumer) pollMessages(ctx context.Context) {
	msgResult, err := c.client.ReceiveMessage(ctx, &sqs.ReceiveMessageInput{
		QueueUrl:            aws.String(c.pendingQueueURL),
		MaxNumberOfMessages: 10,
		WaitTimeSeconds:     5,
	})

	if err != nil {
		log.Printf("Erro ao buscar mensagens na fila SQS: %v", err)
		return
	}

	for _, msg := range msgResult.Messages {
		var payload map[string]string
		if err := json.Unmarshal([]byte(*msg.Body), &payload); err != nil {
			log.Printf("Erro ao decodificar mensagem: %v", err)
			continue
		}

		orderID := payload["id_ordem"]
		log.Printf("Processando PIX da ordem: %s", orderID)

		err = c.useCase.UpdateStatus(ctx, orderID, domain.StatusPago, time.Now())
		if err != nil {
			log.Printf("Erro ao processar PIX %s: %v", orderID, err)
			continue
		}

		_, err = c.client.DeleteMessage(ctx, &sqs.DeleteMessageInput{
			QueueUrl:      aws.String(c.pendingQueueURL),
			ReceiptHandle: msg.ReceiptHandle,
		})
		if err != nil {
			log.Printf("Erro ao deletar mensagem da fila: %v", err)
		} else {
			log.Printf("PIX da ordem %s processado e removido da fila pendente com sucesso", orderID)
		}
	}
}