# rpe-challenge — Sistema de Gestão de Ordens e Pagamentos

Solução completa para o desafio técnico RPE (vaga Fullstack Pleno), composta por dois microsserviços e um frontend, todos orquestrados por um único `docker-compose.yml`.

---

## Por que Monorepo?

Os três serviços fazem parte do **mesmo domínio de negócio** — criação de ordens, processamento de pagamentos e interface do usuário — e se comunicam diretamente entre si (HTTP síncrono + SQS assíncrono). O monorepo permite:

- Subir toda a stack com **um único comando** (`docker compose up --build`)
- Compartilhar o mesmo `docker-compose.yml` e configurações de infra (redes, volumes, LocalStack)
- Facilitar a avaliação: uma única cópia do repositório entrega o sistema funcionando por completo
- Manter os contratos de API e as variáveis de ambiente alinhados entre os serviços sem dependências externas

---

## Serviços

Cada serviço possui seu próprio `README.md` com detalhes de arquitetura, endpoints e decisões técnicas.

### [`order-service`](./order-service/README.md) — Java / Spring Boot · porta `8080`

Microsserviço principal. Gerencia o ciclo de vida das ordens: criação, autenticação JWT, integração síncrona com o `payment-processor` via Feign Client (com Circuit Breaker), consumo da fila SQS para atualização de status PIX e reprocessamento automático de ordens pendentes via scheduler.

### [`payment-processor`](./payment-processor/README.md) — Go / Gin · porta `8081`

Microsserviço de processamento de pagamentos. Recebe ordens do `order-service`, processa cartão de forma síncrona e PIX de forma assíncrona (fila FIFO → worker → fila de resultado).

### [`order-web`](./order-web/README.md) — React / Vite · porta `3000`

Frontend da plataforma. Oferece autenticação JWT, criação de ordens com idempotência, seleção dinâmica de meios de pagamento e acompanhamento de status PIX em tempo real via polling automático.

---

## Stack Tecnológica

| Camada | Tecnologia |
|---|---|
| Backend principal | Java 21 (Virtual Threads), Spring Boot 3.5, Spring Security + JWT, Spring Cloud OpenFeign, Resilience4j, MapStruct, Lombok, SpringDoc/Swagger |
| Backend pagamentos | Go 1.22+, Gin Gonic |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS v4, Axios, React Router v7 |
| Banco relacional | PostgreSQL 16 + Flyway |
| Banco NoSQL | MongoDB 7 |
| Cache | Redis 7 |
| Mensageria | AWS SQS via LocalStack (fila FIFO + fila Standard) |
| Containerização | Docker + Docker Compose |

---

## Infraestrutura

```
┌─────────────────────────────────────────────────────┐
│                    rpe-network                      │
│                                                     │
│  order-web :3000  →  order-service :8080            │
│                            │                        │
│                    ┌───────┴────────┐               │
│                    ↓               ↓                │
│         payment-processor   PostgreSQL + Redis      │
│               :8081                                 │
│                    │                                │
│               LocalStack                            │
│                 :4566                               │
│          (pagamento-pix-pendente.fifo)              │
│          (pagamento-pix-status)                     │
│                    │                                │
│               MongoDB :27017                        │
└─────────────────────────────────────────────────────┘
```

| Container | Imagem | Porta |
|---|---|---|
| `order-web` | build local (nginx) | 3000 |
| `order-service` | build local (JVM) | 8080 |
| `payment-processor` | build local (Go) | 8081 |
| `rpe-postgres` | postgres:16-alpine | 5432 |
| `rpe-redis` | redis:7-alpine | 6379 |
| `rpe-mongodb` | mongo:7 | 27017 |
| `rpe-localstack` | localstack/localstack:3.0 | 4566 |

---

## Como Subir Tudo

### Pré-requisitos

- [Docker](https://docs.docker.com/get-docker/) (com Docker Compose v2 incluso)

### Passo a passo

**1. Clone o repositório:**

```bash
git clone <url-do-repositorio>
cd rpe-challenge
```

**2. (Mac/Linux) Garanta permissão de execução para o script de criação das filas SQS:**

```bash
chmod +x scripts/init-sqs.sh
```

**3. Suba toda a stack:**

```bash
docker compose up --build
```

> Na primeira execução o build pode levar alguns minutos (compilação Java + Go + Node).
> Para execuções seguintes sem mudanças de código, use `docker compose up` (sem `--build`).

**4. Confirme que as filas SQS foram criadas:**

```bash
docker logs rpe-localstack
```

A saída deve terminar com as duas filas listadas e a mensagem `Ready.`:

```json
{
    "QueueUrls": [
        "http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/pagamento-pix-pendente.fifo",
        "http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/pagamento-pix-status"
    ]
}
Ready.
```

**5. Acesse a plataforma:**

| Serviço | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API (Swagger UI) | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |
| payment-processor | http://localhost:8081 |

### Como parar

```bash
docker compose down
```

Os dados dos bancos são mantidos nos volumes `pgdata` e `mongodata`. Para limpar tudo:

```bash
docker compose down -v
```

---

## O Que Você Pode Fazer

### 1. Criar sua conta e fazer login

Acesse `http://localhost:3000`. Na tela de login, clique em **"Criar agora"**, preencha usuário e senha (mínimo 8 caracteres) e confirme. O login é realizado automaticamente após o cadastro.

### 2. Criar uma ordem de pagamento

Na página principal, preencha os dados da ordem:

- **ID do Item** — UUID (use o botão **"Gerar"** para criar um automaticamente)
- **Valor** — qualquer valor positivo
- **Nome e CPF do comprador** — o CPF é validado
- **Meio de pagamento** — carregado dinamicamente da API: PIX, Crédito ou Débito

### 3. Acompanhar o pagamento

| Meio de pagamento | Comportamento |
|---|---|
| **Crédito / Débito** | Resultado imediato: 80% `PAGO`, 20% `RECUSADO` ou `CANCELADO` |
| **PIX** | Status inicial `PENDENTE_PAGAMENTO`; o card atualiza automaticamente a cada 5s até a resolução |

### 4. Consultar ordens por CPF

Use o campo de busca na página principal para listar todas as ordens associadas a um CPF. O polling continua ativo enquanto houver ordens PIX pendentes.

---

## Como os Serviços se Comunicam

```
Usuário
  │
  ▼
order-web (React)
  │  POST /orders + Idempotency-Key
  │  GET  /orders?cpf_comprador=
  │  GET  /payment-methods
  ▼
order-service (Java)
  │
  ├─ Verifica Idempotency-Key no Redis
  ├─ Persiste ordem no PostgreSQL (status: PENDENTE_PAGAMENTO)
  │
  └─ POST /api/v1/payments ──────────► payment-processor (Go)
                                              │
                              ┌───────────────┴────────────────┐
                              │ CARTÃO                         │ PIX
                              │ Processa sincronamente         │ Salva como PENDENTE
                              │ Retorna PAGO/RECUSADO/CANCELADO│ Publica em pagamento-pix-pendente.fifo
                              └───────────────┐                └──────────────────┐
                                              │                                   │
                                              │                         Worker (goroutine)
                                              │                         Consome a fila FIFO
                                              │                         Publica resultado em
                                              │                         pagamento-pix-status
                                              │                                   │
                              ◄──────────────┘          order-service             │
                              Atualiza PostgreSQL  ◄── PixNotificationListener ◄──┘
                                                        (SQS Consumer)
                                                              │
                                                   PATCH /api/v1/payments/status
                                                              │
                                                              ▼
                                                   payment-processor atualiza MongoDB
```

### Resiliência

O `order-service` protege todas as chamadas ao `payment-processor` com um **Circuit Breaker (Resilience4j)**. Se o processador estiver indisponível, a ordem permanece com status `PENDENTE_PAGAMENTO` e um **scheduler** (a cada 60s) retenta automaticamente — sem perda de ordens.
