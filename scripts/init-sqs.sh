#!/bin/bash
echo "Inicializando filas SQS no LocalStack..."

# fila FIFO para os pagamentos pendentes do PIX
awslocal sqs create-queue \
    --queue-name pagamento-pix-pendente.fifo \
    --attributes FifoQueue=true,ContentBasedDeduplication=true

# fila Standard para o status final do PIX
awslocal sqs create-queue \
    --queue-name pagamento-pix-status

echo "Filas criadas com sucesso!"
awslocal sqs list-queues