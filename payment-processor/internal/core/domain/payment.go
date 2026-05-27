package domain

import "time"

type PaymentStatus string
type PaymentMethod string

const (
	StatusPendente PaymentStatus = "PENDENTE PAGAMENTO"
	StatusPago     PaymentStatus = "PAGO"
	StatusCancelado PaymentStatus = "CANCELADO"
	StatusRecusado PaymentStatus = "RECUSADO"
	StatusReprovado PaymentStatus = "REPROVADO"
)

const (
	MethodPix     PaymentMethod = "PIX"
	MethodCredito PaymentMethod = "CREDITO"
	MethodDebito  PaymentMethod = "DEBITO"
)

type Payment struct {
	OrderID        string        `json:"id_ordem" bson:"id_ordem"`
	ItemID         string        `json:"id_item" bson:"id_item"`
	Amount         float64       `json:"valor" bson:"valor"`
	PaymentMethod  PaymentMethod `json:"meio_pagamento" bson:"meio_pagamento"`
	BuyerName      string        `json:"nome_comprador" bson:"nome_comprador"`
	BuyerCPF       string        `json:"cpf_comprador" bson:"cpf_comprador"`
	ProcessingDate time.Time     `json:"data_processamento" bson:"data_processamento"`
	PaymentStatus  PaymentStatus `json:"status_pagamento" bson:"status_pagamento"`
	PaymentDate    *time.Time    `json:"data_pagamento" bson:"data_pagamento"`
}