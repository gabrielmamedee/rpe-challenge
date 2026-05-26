# payment-processor

Microsserviço de processamento de pagamentos desenvolvido em **Go (Golang)**, seguindo os princípios da **Arquitetura Hexagonal**. Responsável por receber ordens do `order-service`, processar pagamentos por cartão de forma síncrona e pagamentos via PIX de forma assíncrona utilizando filas **AWS SQS**.

---

## 🏛️ Arquitetura Hexagonal

```text
                  +---------------------------------------+
                  |               ADAPTERS                |
                  |                                       |
                  |   [Inbound]                           |
                  |    +-------+                          |
                  |    |  Gin  |-----+                    |
                  |    +-------+     |                    |
                  |                  v                    |
                  |            +-----------+              |
                  |            |   PORTS   |              |
                  |            +-----------+              |
                  |                  |                    |
                  |                  v                    |
                  |            +-----------+              |
                  |            |   CORE    |              |
                  |            +-----------+              |
                  |                  |                    |
                  |                  v                    |
                  |            +-----------+              |
                  |            |   PORTS   |              |
                  |            +-----------+              |
                  |                  |                    |
                  |   [Outbound]     v                    |
                  |    +-------+   +-------+              |
                  |    | Mongo |   |  SQS  |              |
                  |    +-------+   +-------+              |
                  +---------------------------------------+
```

### Justificativa das Decisões de Arquitetura

* **Inversão de Dependência (D do SOLID):** O Caso de Uso (`ProcessPaymentUseCase`) interage exclusivamente com interfaces (`ports.PaymentRepository` e `ports.MessageQueue`). Ele desconhece por completo a existência do driver do MongoDB ou do SDK da AWS. Isso blinda a aplicação contra acoplamento e permite substituições de infraestrutura sem dor.
* **Tipagem Forte e Blindagem contra Strings Mágicas:** Foram criados tipos customizados em Go para atuar como Enums estruturados (`PaymentStatus` e `PaymentMethod`), garantindo a integridade dos dados no nível de compilação.
* **Gerenciamento de Ponteiros para Nulabilidade:** O campo `data_pagamento` (`PaymentDate`) na entidade de domínio é mapeado como um ponteiro (`*time.Time`). Isso permite representar nativamente o estado "Nulo" no banco de dados e no JSON enquanto o pagamento estiver pendente, evitando o uso de "datas mágicas" (como 01/01/0001).
* **Propagação de Contexto (`context.Context`):** Todas as assinaturas de funções de I/O (repositório e mensageria) herdam obrigatoriamente o contexto HTTP da requisição. Se um cliente abortar uma chamada, o Go propaga esse cancelamento para o driver do banco e da fila, otimizando o uso de recursos de infraestrutura.

---

## ⚡ Fluxos de Processamento e Regras de Negócio

O microsserviço divide as operações em duas grandes esteiras:

### 1. Esteira Síncrona: Cartão de Crédito / Débito

Quando a requisição chega com o método `CREDITO` ou `DEBITO`, o fluxo é liquidado imediatamente na mesma requisição (HTTP 201).

* **Simulação Estatística Controlada (Diferencial):** O teste exigia uma simulação probabilística de falhas onde 1 a cada 5 requisições retornasse falha. Implementamos um algoritmo estatístico baseado em distribuição probabilística matemática. Em uma amostra representativa, a taxa de erro é travada exatamente em **20%** (distribuída uniformemente entre `RECUSADO` e `CANCELADO`), enquanto as demais **80%** das requisições são marcadas como `PAGO`.

### 2. Esteira Assíncrona: PIX e Resiliência com SQS

Quando a requisição chega com o método `PIX`, a regra de negócio exige processamento distribuído:

* O Caso de Uso armazena a ordem no MongoDB com o status inicial `PENDENTE PAGAMENTO`.
* O serviço atua como **Produtor (Producer)** e publica o ID da ordem imediatamente na fila SQS FIFO (`pagamento-pix-pendente.fifo`).
* A API responde imediatamente HTTP 201 com o status `PENDENTE`, liberando o cliente.
* Em segundo plano, uma **Goroutine dedicada (Thread leve nativa do Go)** atua como **Consumidor (Worker / SQSConsumer)** escutando a fila FIFO por meio de *Long Polling* (`WaitTimeSeconds: 5`) para economizar CPU e custos de nuvem.
* O Worker captura a mensagem de forma garantida e sequencial (respeitando as travas de desduplicação `MessageDeduplicationId` nativas da fila FIFO), simula a aprovação do PIX, altera o status no MongoDB para `PAGO` e envia o resultado final para a segunda fila SQS Standard (`pagamento-pix-status`), concluindo o ciclo assíncrono.

---

## 🛠️ Tecnologias Utilizadas

* **Language:** Go (Golang) v1.22 (Alinhado via `go.mod` para builds determinísticos)
* **Web Framework:** Gin Gonic v1.10 (Roteamento minimalista, de ultra baixa latência e sem alocações desnecessárias de memória)
* **Database:** MongoDB Driver Oficial (Armazenamento em formato BSON de documentos de pagamento)
* **Cloud Integration:** AWS SDK for Go v2 (Pacotes de `config` e `service/sqs` oficiais)
* **Environment Management:** GoDotEnv (Injeção limpa de `.env` apenas em ambiente de desenvolvimento local)
* **Containerization:** Docker & Docker Compose com **Multi-Stage Build** (Reduz a imagem de produção de ~800MB para escassos **~18MB**, copiando apenas o executável estático compilado em cima de uma imagem Alpine ultra-safe)

---

## 📁 Estrutura do Projeto

Seguindo o padrão idiomático da comunidade Go de separação de conceitos por coesão:

```text
payment-processor/
├── cmd/
│   └── api/
│       └── main.go             # Ponto de entrada (Entrypoint), Injeção de Dependências manual
├── internal/
│   ├── adapters/               # Adaptadores (As Bordas do Hexágono)
│   │   ├── inbound/            # Entrada (Quem nos chama)
│   │   │   ├── http/
│   │   │   │   └── handlers/   # Controladores Gin e DTOs de Request/Response
│   │   │   └── messaging/
│   │   │       └── sqs/        # Worker / Consumer que escuta a fila do PIX
│   │   └── outbound/           # Saída (Quem nós chamamos)
│   │       ├── messaging/
│   │       │   └── sqs/        # Publisher que posta mensagens nas filas SQS
│   │       └── repository/
│   │           └── mongodb/    # Implementação de Repositório no MongoDB
│   └── core/                   # O Coração do Hexágono (Independente de infra)
│       ├── domain/             # Entidades de Domínio, Tipos Customizados (Enums)
│       └── ports/              # Interfaces/Contratos (Inversão de Dependência)
├── .env                        # Variáveis de ambiente locais (Ignorado no Git)
├── Dockerfile                  # Configuração de build Multi-stage otimizado
├── go.mod                      # Gerenciador de módulos e versões do projeto
└── go.sum                      # Assinaturas e Hashes de segurança das dependências
```

---

## 🚀 Como Executar o Projeto

Toda a infraestrutura do projeto já está integrada no `docker-compose.yml` unificado na raiz principal do desafio.

### Pré-requisitos

Certifique-se de que o **Docker** e o **Docker Compose** estejam instalados e rodando na sua máquina.

### Executando em ambiente Docker (Recomendado)

A partir da pasta raiz do desafio (onde reside o arquivo `docker-compose.yml`), execute o comando para compilar e subir todos os serviços orquestrados:

```bash
docker-compose up -d --build
```

O Docker irá compilar isoladamente o binário em Go e subirá o container do `payment-processor` na porta **8081**, garantindo que a porta 8080 fique livre para o `order-service` em Java.

Para acompanhar os logs do microsserviço Go (incluindo as mensagens de polling do Worker SQS), execute:

```bash
docker logs -f payment-processor
```

---

## 🧪 Testes Unitários e Estatísticos

Os testes foram escritos utilizando o pacote nativo `testing` do Go, dispensando frameworks pesados de terceiros e priorizando a velocidade extrema (os testes rodam em escassos milissegundos).

Incluímos um **teste estatístico avançado** de distribuição probabilística para provar cientificamente que a função de aleatoriedade atende ao requisito de falhas do Cartão de Crédito.

### Como Rodar os Testes

Certifique-se de estar dentro da pasta do microsserviço (`payment-processor/`) e execute:

```bash
go test -v ./...
```

### O que é Validado

* **`TestProcessPaymentUseCase_Pix`:** Garante que se o pagamento for PIX, ele salva no banco como pendente e dispara obrigatoriamente o publisher da fila FIFO, sem tocar no fluxo de cartões.

* **`TestProcessPaymentUseCase_Card`:** Garante que pagamentos em cartão alteram o status de forma síncrona, atualizam o banco de dados e jamais injetam lixo nas filas SQS.

* **`TestGenerateRandomCardStatus_Distribution`:** Roda a regra de aleatoriedade 10.000 vezes em background em menos de 1 milissegundo e valida se a margem de erro estatística flutua estritamente na casa dos 20% exigidos pelo documento de requisitos.

---

## 🛰️ Validação Manual dos Endpoints

### 1. Testando Cartão de Crédito (Fluxo Síncrono)

```bash
curl -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "id_ordem": "ORD-CARD-001",
    "id_item": "ITEM-ABC",
    "valor": 299.90,
    "meio_pagamento": "CREDITO",
    "nome_comprador": "Alexandre Mamede",
    "cpf_comprador": "12345678901"
  }'
```

### 2. Testando PIX (Fluxo Assíncrono com Fila)

```bash
curl -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "id_ordem": "ORD-PIX-002",
    "id_item": "ITEM-XYZ",
    "valor": 450.00,
    "meio_pagamento": "PIX",
    "nome_comprador": "Fulano de Tal",
    "cpf_comprador": "98765432100"
  }'
```

Ao disparar o comando do PIX, observe imediatamente os logs do container (`docker logs -f payment-processor`). Você testemunhará o Worker capturando a ordem de pagamento da fila SQS FIFO local, realizando a liquidação assíncrona e limpando a fila automaticamente.

---

## 🛠️ Validação via AWS CLI

Caso possua o `aws-cli` instalado e deseje interagir diretamente com o broker simulado pelo LocalStack:

**Listar Filas Criadas:**
```bash
aws --endpoint-url=http://localhost:4566 sqs list-queues
```

**Ler Mensagens da Fila de Status Final (Standard):**
```bash
aws --endpoint-url=http://localhost:4566 sqs receive-message \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/pagamento-pix-status \
  --max-number-of-messages 10
```