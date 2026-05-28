# payment-processor

Microsserviço de processamento de pagamentos escrito em **Go**, seguindo **Arquitetura Hexagonal**. Responsável por receber ordens de pagamento, processá-las de forma síncrona (cartão) ou assíncrona (PIX) e persistir os resultados no MongoDB.

---

## Recursos e Funcionalidades

### Processamento de Cartão (Síncrono)
Pagamentos via `CREDITO` ou `DEBITO` são liquidados na mesma requisição. O serviço aplica uma **simulação estatística controlada**: 80% das transações resultam em `PAGO` e 20% em falha (`RECUSADO` ou `CANCELADO`), distribuídos uniformemente em uma amostra representativa.

### Processamento PIX (Assíncrono)
Pagamentos via `PIX` seguem um fluxo distribuído:
1. A ordem é salva no MongoDB com status `PENDENTE PAGAMENTO` e o cliente recebe HTTP 201 imediatamente.
2. O serviço publica o `id_ordem` na fila SQS FIFO `pagamento-pix-pendente.fifo`.
3. Uma **goroutine dedicada** (Worker) escuta a fila via *long polling* e finaliza o pagamento de forma assíncrona.
4. O resultado final é publicado na fila SQS Standard `pagamento-pix-status`.

### Idempotência e Resiliência
- O `id_ordem` é usado como `MessageDeduplicationId` na fila FIFO, impedindo o processamento duplicado da mesma ordem mesmo em cenários de retry.
- O Worker deleta a mensagem da fila **somente após** o processamento bem-sucedido, garantindo semântica *at-least-once delivery*.

### Domínio Rico
Status de pagamento com semântica distinta por método: cartão com falha retorna `RECUSADO`, PIX com falha retorna `REPROVADO` — refletindo a natureza de cada recusa no domínio financeiro.

---

## Tecnologias Utilizadas

| Tecnologia | Versão | Uso |
|---|---|---|
| Go | 1.22+ | Linguagem principal |
| Gin Gonic | v1.10 | Framework HTTP |
| MongoDB Driver | v1.17 | Persistência de pagamentos |
| AWS SDK for Go | v2 | Integração com SQS |
| LocalStack | — | Simulação local da AWS (SQS) |
| GoDotEnv | v1.5 | Variáveis de ambiente locais |
| Docker | Multi-stage | Imagem de produção |

---

## Capacidades

- Processar pagamentos via **Cartão de Crédito**, **Débito** e **PIX**
- Gerenciar dois fluxos distintos: **síncrono** (cartão) e **assíncrono** (PIX com filas)
- Publicar e consumir mensagens em filas **SQS FIFO** e **SQS Standard**
- Garantir **idempotência** via deduplicação nativa da fila FIFO
- Simular aprovação/recusa de pagamentos com **distribuição probabilística controlada** (20% de falha)
- Propagar cancelamento de contexto HTTP até o banco de dados e a fila (via `context.Context`)
- Expor API REST para processamento e atualização manual de status

---

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/v1/payments` | Processa um novo pagamento |
| `PATCH` | `/api/v1/payments/status` | Atualiza o status de um pagamento existente |

---

## Estrutura do Projeto

```
payment-processor/
├── cmd/api/main.go                          # Entrypoint e injeção de dependências
├── internal/
│   ├── core/
│   │   ├── domain/payment.go               # Entidade de domínio e enums de status
│   │   └── ports/ports.go                  # Interfaces (PaymentRepository, MessageQueue)
│   ├── application/usecases/
│   │   ├── process_payment.go              # Caso de uso principal
│   │   └── process_payment_test.go         # Testes unitários e estatísticos
│   └── adapters/
│       ├── inbound/
│       │   ├── http/handlers/              # Handlers Gin (HTTP)
│       │   └── messaging/sqs/              # Worker SQS consumer (goroutine)
│       └── outbound/
│           ├── repository/mongodb/         # Implementação do repositório MongoDB
│           └── messaging/sqs/              # Publisher SQS
├── Dockerfile
└── go.mod
```

---

## Como Executar

Na raiz do monorepo, com Docker instalado:

```bash
docker-compose up -d --build
```

O serviço sobe na porta **8081**. Para acompanhar os logs do Worker SQS:

```bash
docker logs -f payment-processor
```

---

## Testes

```bash
# Dentro de payment-processor/
go test -v ./...
```

| Teste | O que valida |
|---|---|
| `TestProcessPaymentUseCase_Pix` | PIX salva como pendente e dispara a fila FIFO |
| `TestProcessPaymentUseCase_Card` | Cartão atualiza status de forma síncrona, sem tocar na fila |
| `TestGenerateRandomCardStatus_Distribution` | Distribuição de falhas entre 18%–22% em 10.000 iterações |
| `TestProcessPaymentUseCase_UpdateStatus` | Atualização direta de status delega ao repositório |
| `TestProcessPaymentUseCase_CompletePixProcess` | Finalização do PIX atualiza banco e publica na fila de status |
