

com.rpe.orderservice
├── core                        # O Hexágono (Regras de Negócio)
│   ├── domain                  # Entidades, Enums e Exceções de Domínio
│   └── ports                   # Contratos/Interfaces
│       ├── inbound             # Interfaces que os Casos de Uso implementam (ex: CreateOrderUseCase)
│       └── outbound            # Interfaces que a infraestrutura implementa (ex: OrderRepositoryPort)
├── adapters                    # A Infraestrutura (Frameworks, BD, APIs)
│   ├── inbound                 # Quem chama a nossa aplicação (REST Controllers, SQS Listeners)
│   └── outbound                # Quem a nossa aplicação chama (Spring Data Repositories, Feign Clients)
└── config                      # Configurações globais (Beans, Security, Error Handler, Redis)

Order Service - Sistema de Processamento de Pedidos Resiliente
Este projeto é um microsserviço de gerenciamento de ordens de compra, desenvolvido como resolução de um desafio técnico focado em alta disponibilidade, resiliência e boas práticas de engenharia de software.

O sistema simula um e-commerce ou gateway de pagamento, recebendo pedidos de forma síncrona, integrando-se com serviços externos e processando atualizações de status de forma assíncrona via mensageria.

🏛️ Arquitetura e Padrões de Projeto
A aplicação foi construída sob os rigorosos padrões da Arquitetura Hexagonal (Ports and Adapters) combinada com conceitos de Domain-Driven Design (DDD).

O coração do sistema (Core/Domain) é 100% isolado de frameworks, banco de dados ou detalhes de infraestrutura, garantindo alta testabilidade e facilidade de manutenção.

✨ Diferenciais Técnicos Implementados
Idempotência Distribuída: Implementação de trava de segurança utilizando Redis (via Idempotency-Key header) com TTL de 5 minutos, protegendo a API contra ataques de repetição ("duplo clique") e garantindo consistência em retentativas de rede.

Mensageria e Eventos: Consumo assíncrono de eventos de pagamento através do AWS SQS (emulado via LocalStack), permitindo o processamento em background sem travar a thread do usuário.

Circuit Breaker (Self-Healing): Uso do Resilience4j na comunicação síncrona com microsserviços externos (via Feign Client). Em caso de falhas ou indisponibilidade, o sistema aciona métodos de Fallback e adota processamento tolerante a falhas.

Tratamento de Exceções Global: API blindada com um @ControllerAdvice, garantindo retornos JSON padronizados (RFC 7807) para erros de domínio (400, 409) sem vazar StackTraces.

Evolução Contínua de Banco de Dados: Gerenciamento de schema automatizado via Flyway, garantindo que o PostgreSQL esteja sempre na versão correta.

🛠️ Stack Tecnológica
Linguagem: Java 21

Framework Base: Spring Boot 3.5.x

Banco de Dados Relacional: PostgreSQL (com Spring Data JPA e Flyway)

Cache & Lock Distribuído: Redis (com Spring Data Redis)

Mensageria: AWS SQS (Spring Cloud AWS + LocalStack)

Comunicação REST: OpenFeign

Resiliência: Resilience4j (CircuitBreaker)

Mapeamento de Objetos: MapStruct / Lombok

Testes: JUnit 5, Mockito, MockMvc

📂 Estrutura de Diretórios (Hexágono)
Plaintext
src/main/java/com/rpe/orderservice/
├── adapters/          # Adaptadores (Implementações das Portas)
│   ├── inbound/       # Entrada: Controllers REST, Listeners SQS (HTTP/Mensageria -> Core)
│   └── outbound/      # Saída: Repositories JPA, Clients Feign (Core -> Banco/APIs)
├── core/              # O Coração da Aplicação (Isolado de frameworks)
│   ├── domain/        # Entidades de Negócio, Enums e Exceções de Domínio
│   ├── ports/         # Contratos (Interfaces Inbound e Outbound)
│   └── usecases/      # Casos de Uso (As regras de negócio em si)
└── OrderServiceApplication.java
🚀 Como Executar o Projeto
1. Subindo a Infraestrutura (Docker)
O projeto depende de PostgreSQL, Redis e LocalStack. Certifique-se de ter o Docker instalado e rodando.
Na raiz do projeto (onde está o arquivo docker-compose.yml da arquitetura), execute:

Bash
docker-compose up -d
2. Criando a Fila SQS no LocalStack
Bash
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name pagamento-pix-status --region us-east-1
3. Executando a Aplicação Spring Boot
Não é necessário ter o Maven instalado na máquina, utilize o wrapper incluso:

Bash
./mvnw clean compile spring-boot:run
(A aplicação estará disponível na porta 8080)

📖 Documentação da API
1. Criar Nova Ordem de Compra
Cria um pedido, verifica a idempotência no Redis e tenta integração síncrona. Em caso de sucesso, aguarda atualização via SQS.

Rota: POST /api/v1/orders

Headers:

Authorization: Bearer <token_jwt>

Idempotency-Key: <uuid_gerado_pelo_front>

Body:

JSON
{
  "id_item": "123e4567-e89b-12d3-a456-426614174999",
  "valor": 30.50,
  "meio_pagamento": "PIX",
  "nome_comprador": "Carlos Souza",
  "cpf_comprador": "98765432107"
}
2. Consultar Histórico de Ordens
Busca todas as ordens de um comprador de forma simplificada.

Rota: GET /api/v1/orders?cpf_comprador=98765432107

Headers: Authorization: Bearer <token_jwt>

Response (200 OK):

JSON
[
  {
    "id": "a364d3f3-99bf-4e4d-a269-ca9239f6906c",
    "nome_comprador": "Carlos Souza",
    "status": "PAGO"
  }
]
🧪 Cobertura de Testes
O projeto conta com uma robusta suíte de testes unitários focados na camada Core e testes de integração para os adaptadores de entrada (MockMvc).
Para rodar os testes:

Bash
./mvnw test
Desenvolvido com foco em engenharia de software de alta performance e resiliência.

[Postman] -> (POST /orders)
   │
   ▼
[Java: order-service]
   │ 1. Salva no Postgres como PENDENTE_PAGAMENTO
   │ 2. Dispara chamada HTTP Feign para o Go
   ▼
[Go: payment-processor]
   │ 1. Recebe a chamada e salva no MongoDB
   │ 2. Posta o ID na fila FIFO 'pagamento-pix-pendente'
   │ 3. O próprio Listener do Go consome essa fila FIFO
   │ 4. O Go processa o status (Random) e posta na fila 'pagamento-pix-status'
   ▼
[LocalStack: SQS] (Mensagem trafega de uma fila para a outra)
   ▼
[Java: order-service]
     1. O seu Listener SQS detecta a mensagem automaticamente
     2. O UseCase atualiza o Postgres para PAGO/CANCELADO/RECUSADO
     3. O Java dispara o Feign de volta (PATCH) para o Go atualizar o MongoDB