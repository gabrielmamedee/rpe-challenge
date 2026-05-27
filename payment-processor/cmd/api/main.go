package main

import (
	"context"
	"log"
	"os"
	"time"

	httpHandlers "payment-processor/internal/adapters/inbound/http/handlers"
	sqsConsumer "payment-processor/internal/adapters/inbound/messaging/sqs"
	sqsPublisher "payment-processor/internal/adapters/outbound/messaging/sqs"
	mongoRepo "payment-processor/internal/adapters/outbound/repository/mongodb"
	"payment-processor/internal/application/usecases"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	awssqs "github.com/aws/aws-sdk-go-v2/service/sqs"
	"github.com/gin-gonic/gin"
	"github.com/joho/godotenv"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

func main() {

	if err := godotenv.Load(); err != nil {
		log.Println("Aviso: Arquivo .env não encontrado. Utilizando variáveis de ambiente nativas do sistema.")
	}

	ctxMongo, cancelMongo := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancelMongo()

	mongoURI := os.Getenv("MONGO_URI")
	if mongoURI == "" {
		mongoURI = "mongodb://admin:adminpassword@localhost:27017"
	}

	mongoClient, err := mongo.Connect(ctxMongo, options.Client().ApplyURI(mongoURI))
	if err != nil {
		log.Fatalf("Erro crítico: Não foi possível conectar ao MongoDB: %v", err)
	}
	defer mongoClient.Disconnect(context.Background())

	db := mongoClient.Database("rpe_payments")

	awsRegion := os.Getenv("AWS_REGION")
	if awsRegion == "" {
		awsRegion = "us-east-1"
	}

	awsCfg, err := config.LoadDefaultConfig(context.TODO(),
		config.WithRegion(awsRegion),
	)
	if err != nil {
		log.Fatalf("Erro crítico: Não foi possível carregar as configurações da AWS: %v", err)
	}

	sqsClient := awssqs.NewFromConfig(awsCfg, func(o *awssqs.Options) {
		sqsEndpoint := os.Getenv("AWS_ENDPOINT_URL")
		if sqsEndpoint == "" {
			sqsEndpoint = "http://localhost:4566"
		}
		o.BaseEndpoint = aws.String(sqsEndpoint)
	})

	pendingQueueURL := "http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/pagamento-pix-pendente.fifo"
	statusQueueURL := "http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/pagamento-pix-status"

	repo := mongoRepo.NewPaymentRepository(db)
	publisher := sqsPublisher.NewSQSPublisher(sqsClient, pendingQueueURL, statusQueueURL)

	useCase := usecases.NewProcessPaymentUseCase(repo, publisher)

	consumer := sqsConsumer.NewSQSConsumer(sqsClient, pendingQueueURL, useCase)
	
	workerCtx := context.Background()
	go consumer.Start(workerCtx)
	
	handler := httpHandlers.NewPaymentHandler(useCase)
	router := gin.Default()

	api := router.Group("/api/v1")
	{
		api.POST("/payments", handler.ProcessPayment)
		api.PATCH("/payments/status", handler.UpdateStatus)
	}

	port := os.Getenv("PORT")
	if port == "" {
		port = "8081"
	}

	log.Printf("Servidor payment-processor inicializado com sucesso na porta %s...", port)
	if err := router.Run(":" + port); err != nil {
		log.Fatalf("Erro crítico ao iniciar o servidor HTTP Gin: %v", err)
	}
}