package handlers

import (
	"github.com/gin-gonic/gin"
)

func respondWithError(c *gin.Context, statusCode int, message string, err error) {
	details := ""
	if err != nil {
		details = err.Error()
	}

	c.JSON(statusCode, gin.H{
		"statusCode": statusCode,
		"message":    message,
		"details":    details,
	})
}