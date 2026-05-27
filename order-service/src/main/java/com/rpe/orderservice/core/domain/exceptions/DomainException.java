package com.rpe.orderservice.core.domain.exceptions;

public class DomainException extends RuntimeException{
    public DomainException(String message) {
        super(message);
    }
}
