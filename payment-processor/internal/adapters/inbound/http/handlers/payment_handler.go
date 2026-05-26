package handlers

import (
	"net/http"
	"time"

	"payment-processor/internal/application/usecases"
	"payment-processor/internal/core/domain"

	"github.com/gin-gonic/gin"
)

type PaymentHandler struct {
	useCase *usecases.ProcessPaymentUseCase
}

func NewPaymentHandler(uc *usecases.ProcessPaymentUseCase) *PaymentHandler {
	return &PaymentHandler{
		useCase: uc,
	}
}


// Rota: POST /payments
func (h *PaymentHandler) ProcessPayment(c *gin.Context) {
	var payment domain.Payment

	if err := c.ShouldBindJSON(&payment); err != nil {
		respondWithError(c,http.StatusBadRequest, "Payload inválido ou campos faltando", err)
		return
	}

	if err := h.useCase.Execute(c.Request.Context(), &payment); err != nil {
		respondWithError(c,http.StatusInternalServerError, "Erro interno ao processar o pagamento", err)
		return
	}

	c.JSON(http.StatusCreated, payment)
}

type UpdateStatusRequest struct {
	OrderID       string               `json:"id_ordem"`
	PaymentStatus domain.PaymentStatus `json:"status_pagamento"`
	PaymentDate   time.Time            `json:"data_pagamento"`
}

// Rota: PATCH /payments/status
func (h *PaymentHandler) UpdateStatus(c *gin.Context) {
	var req UpdateStatusRequest

	if err := c.ShouldBindJSON(&req); err != nil {
		respondWithError(c,http.StatusBadRequest, "Payload inválido", err)
		return
	}

	if err := h.useCase.UpdateStatus(c.Request.Context(), req.OrderID, req.PaymentStatus, req.PaymentDate); err != nil {
		respondWithError(c,http.StatusInternalServerError, "Erro ao atualizar status do pagamento", err)
		return
	}

	c.Status(http.StatusNoContent)
}