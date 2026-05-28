# Order Service

Microsserviço responsável pelo gerenciamento do ciclo de vida das ordens de compra dentro do ecossistema de pagamentos do desafio RPE. Desenvolvido em **Java 21** com **Spring Boot 3.5**, o serviço centraliza a criação de ordens, a integração síncrona com o processador de pagamentos e o processamento assíncrono de atualizações de status via mensageria, operando sob os princípios da **Arquitetura Hexagonal (Ports & Adapters)** para garantir isolamento total do domínio em relação à infraestrutura.

---

## Recursos Diferenciais

### Worker de Reprocessamento de Pedidos

Um scheduler interno (`OrderReprocessingScheduler`) executa a cada **60 segundos** buscando todas as ordens em status `PENDENTE_PAGAMENTO`. Para cada uma delas, o serviço retenta a integração com o `payment-processor`. Isso resolve automaticamente os casos em que a chamada síncrona inicial falhou por indisponibilidade do serviço externo, sem intervenção manual. Ordens que continuam sem resposta permanecem pendentes até o próximo ciclo.

```
@Scheduled(fixedDelay = 60000)
→ Busca ordens PENDENTE_PAGAMENTO
→ Retenta processPayment via Feign
→ Persiste novo status se diferente de PENDENTE_PAGAMENTO
```

### Idempotência Distribuída

O endpoint `POST /orders` exige o header `Idempotency-Key`. Ao receber a requisição, o controller realiza um `SET NX` no Redis com a chave `idemp_order:<valor>` e TTL de **3 minutos**. Se a chave já existir, a requisição é rejeitada com erro `409`, prevenindo criação de ordens duplicadas em cenários de duplo clique, retry automático do cliente ou instabilidade de rede.

A chave é removida do Redis em caso de falha no processamento, permitindo que o cliente reenvie legitimamente a mesma `Idempotency-Key` após um erro.

### Cache com Redis

A listagem de meios de pagamento (`GET /payment-methods`) utiliza `@Cacheable` com TTL de **30 minutos**, armazenando os dados em Redis com serialização JSON. Como esses dados raramente mudam, o cache elimina leituras repetidas ao banco de dados em cada requisição do frontend.

```
GET /payment-methods
→ Verifica cache Redis (chave: payment_methods)
→ Cache HIT: retorna sem tocar no banco
→ Cache MISS: consulta PostgreSQL e armazena por 30 minutos
```

### Feign Client com Circuit Breaker

A comunicação síncrona com o `payment-processor` (Go) é feita via **Spring Cloud OpenFeign**. O `PaymentIntegrationAdapter` envolve a chamada com um **Circuit Breaker Resilience4j**, configurado com:

| Parâmetro | Valor |
|---|---|
| Janela de avaliação | 10 chamadas |
| Taxa de falha para abrir | 50% |
| Tempo em estado aberto | 10 segundos |
| Chamadas no estado semi-aberto | 3 |
| Timeout por chamada | 3 segundos |

Quando o circuito abre, o método `fallbackProcessPayment` é acionado, mantendo a ordem com status `PENDENTE_PAGAMENTO` — que será retomada pelo worker de reprocessamento assim que o serviço externo se recuperar.

### Mensageria com AWS SQS

O `PixNotificationListener` escuta a fila `pagamento-pix-status` (via LocalStack em desenvolvimento) usando `@SqsListener`. Quando o `payment-processor` (Go) finaliza o processamento de um pagamento PIX, ele publica uma mensagem nessa fila com o ID da ordem e o status resultante.

O listener recebe a mensagem, executa o `UpdateOrderStatusUseCase`, persiste o novo status no PostgreSQL e dispara um **callback via Feign** (`PATCH /api/v1/payments/status`) de volta ao `payment-processor` para sincronizar o estado no MongoDB.

```
[Go: payment-processor]
    └── publica em pagamento-pix-status
          ↓
[PixNotificationListener]
    └── UpdateOrderStatusUseCase
          ├── persiste no PostgreSQL
          └── callback PATCH → payment-processor
```

### Spring Boot Actuator

O Actuator está habilitado com exposição dos endpoints `health` e `info`:

- `GET /actuator/health` — estado da aplicação e dependências
- `GET /actuator/info` — metadados do serviço (nome, descrição, versão)

---

## Estrutura de Pastas

```
src/main/java/com/rpe/orderservice/
│
├── core/                              # Domínio isolado de frameworks
│   ├── domain/                        # Entidades de negócio e enums
│   │   ├── Order.java                 # Entidade principal com lógica de updateStatus
│   │   ├── PaymentStatus.java         # Enum: PENDENTE_PAGAMENTO, PAGO, CANCELADO, RECUSADO, REPROVADO
│   │   ├── PaymentMethod.java         # Enum: PIX, CREDITO, DEBITO
│   │   └── exceptions/                # DomainException, ResourceNotFoundException
│   ├── ports/
│   │   ├── inbound/                   # Interfaces dos casos de uso (entrada)
│   │   └── outbound/                  # Interfaces de repositórios e integrações (saída)
│   └── usecases/                      # Implementações dos casos de uso
│       ├── CreateOrderUseCaseImpl
│       ├── UpdateOrderStatusUseCaseImpl
│       ├── ReprocessPendingOrdersUseCaseImpl
│       ├── FindOrdersByBuyerCpfUseCaseImpl
│       ├── ListPaymentOptionsUseCaseImpl
│       └── CreateUserUseCaseImpl
│
├── adapters/
│   ├── inbound/
│   │   ├── http/                      # Controllers REST + DTOs + MapStruct mappers
│   │   │   ├── OrderController        # POST /orders, GET /orders
│   │   │   ├── AuthController         # POST /login
│   │   │   ├── UserController         # POST /users
│   │   │   └── PaymentMethodController# GET /payment-methods
│   │   ├── sqs/                       # Listener SQS
│   │   │   └── PixNotificationListener
│   │   └── scheduler/                 # Scheduler de reprocessamento
│   │       └── OrderReprocessingScheduler
│   └── outbound/
│       ├── http/                      # Feign clients para o payment-processor
│       │   ├── PaymentProcessorClient
│       │   ├── PaymentIntegrationAdapter  # Circuit Breaker + fallback
│       │   └── PaymentCallbackAdapter     # Callback de status
│       └── repository/                # Adapters JPA + entidades de banco
│           ├── OrderRepositoryAdapter
│           ├── UserRepositoryAdapter
│           └── PaymentOptionRepositoryAdapter
│
├── config/
│   ├── security/                      # Spring Security + JWT (TokenService, SecurityFilter)
│   ├── CacheConfig.java               # Redis: TTL 30min, serialização JSON
│   ├── GlobalExceptionHandler.java    # @ControllerAdvice com respostas padronizadas
│   └── web/WebConfig.java             # CORS
│
└── OrderServiceApplication.java
```

---

## Documentação Interativa (Swagger)

A API está documentada com **SpringDoc OpenAPI** e pode ser explorada via Swagger UI após subir a aplicação:

```
http://localhost:8080/swagger-ui.html
```

---

## Recursos da API

Todos os endpoints (exceto `/login` e `/users`) exigem autenticação via `Authorization: Bearer <token>`.

### Autenticação

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/login` | Autentica e retorna JWT |
| `POST` | `/users` | Cria novo usuário |

**POST /login**
```json
// Request
{ "login": "usuario", "password": "senha" }

// Response 200
{ "token": "<jwt>", "type": "Bearer" }
```

### Ordens

| Método | Rota | Headers obrigatórios | Descrição |
|---|---|---|---|
| `POST` | `/orders` | `Authorization`, `Idempotency-Key` | Cria uma ordem e inicia o pagamento |
| `GET` | `/orders?cpf_comprador=` | `Authorization` | Lista ordens de um comprador pelo CPF |

**POST /orders**
```json
// Request
{
  "id_item": "123e4567-e89b-12d3-a456-426614174999",
  "valor": 150.00,
  "meio_pagamento": "PIX",
  "nome_comprador": "Maria Silva",
  "cpf_comprador": "12345678901"
}

// Response 201
{
  "id": "a364d3f3-99bf-4e4d-a269-ca9239f6906c",
  "status": "PENDENTE_PAGAMENTO",
  "criado_em": "2025-01-15T10:30:00"
}
```

### Meios de Pagamento

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/payment-methods` | Lista os meios disponíveis (PIX, CREDITO, DEBITO) — resposta cacheada |

### Monitoramento

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/actuator/health` | Status de saúde da aplicação |
| `GET` | `/actuator/info` | Metadados do serviço |

---

## Fluxo Completo de Pagamento

```
POST /orders
    │
    ├─ 1. Verifica Idempotency-Key no Redis
    ├─ 2. Valida regras de domínio (valor, CPF, nome)
    ├─ 3. Persiste ordem como PENDENTE_PAGAMENTO no PostgreSQL
    └─ 4. Chama payment-processor via Feign (Circuit Breaker)
              │
              ├─ [SUCESSO] Atualiza status com resposta síncrona
              │
              └─ [FALHA / CIRCUIT OPEN]
                    └─ Mantém PENDENTE_PAGAMENTO
                         └─ Worker (60s) retenta automaticamente

[payment-processor publica em SQS: pagamento-pix-status]
    │
    └─ PixNotificationListener
          ├─ Atualiza status no PostgreSQL
          └─ Callback PATCH → payment-processor (sincroniza MongoDB)
```
