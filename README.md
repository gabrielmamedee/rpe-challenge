# Desafio Técnico - Sistema de Gestão de Ordens e Pagamentos

Este projeto consiste em uma solução distribuída para o gerenciamento de ordens de serviço e processamento de pagamentos, composta por dois microsserviços (Java/Spring Boot e Golang) e um front-end (React).

---

## 🛠️ Etapa 1: Infraestrutura Local

Para garantir a portabilidade e a facilidade de teste, toda a infraestrutura necessária (bancos de dados, cache e mensageria) foi conteinerizada utilizando **Docker** e **Docker Compose**.

### Componentes da Infraestrutura:
* **PostgreSQL 16:** Banco de dados relacional utilizado pelo `order-service`.
* **MongoDB 7:** Banco de dados NoSQL utilizado pelo `payment-processor`.
* **Redis 7:** Camada de cache para otimização de consultas a meios de pagamento.
* **LocalStack (AWS SQS):** Simulação local do serviço AWS SQS para mensageria assíncrona, configurado automaticamente com duas filas:
    * `pagamento-pix-pendente.fifo` (Fila FIFO para garantir a ordem estrita no início do fluxo do PIX).
    * `pagamento-pix-status` (Fila Standard para notificações de atualização de status).

---

## 🚀 Como Subir a Infraestrutura

### Pré-requisitos
Certifique-se de ter instalado em sua máquina:
* [Docker](https://docs.docker.com/get-docker/)

### Passo a Passo

1. **Clone o repositório** (ou navegue até a pasta raiz do projeto):
   ```bash
   cd desafio-rpe
   ```

2. **(Apenas Mac/Linux)** Garanta permissão de execução para o script de inicialização do SQS:
   ```bash
   chmod +x scripts/init-sqs.sh
   ```

3. **Suba os containers em segundo plano:**
   ```bash
   docker-compose up -d
   ```

4. **Verifique se a infraestrutura inicializou com sucesso:**

   Aguarde alguns segundos e execute o comando abaixo para visualizar os logs do LocalStack e confirmar a criação das filas SQS:
   ```bash
   docker logs rpe-localstack
   ```

   O output do terminal deve terminar com a lista das filas criadas e a mensagem `Ready.`, conforme o exemplo:
   ```json
   {
       "QueueUrls": [
           "http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/pagamento-pix-pendente.fifo",
           "http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/pagamento-pix-status"
       ]
   }
   Ready.
   ```

---

## 🛑 Como Parar a Infraestrutura

Para encerrar os serviços e liberar as portas do seu computador sem perder os dados persistidos nos bancos de dados, execute:
```bash
docker-compose down
```

---
