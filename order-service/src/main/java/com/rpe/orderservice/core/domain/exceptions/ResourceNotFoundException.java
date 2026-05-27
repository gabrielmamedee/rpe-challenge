package com.rpe.orderservice.core.domain.exceptions;

public class ResourceNotFoundException extends DomainException{
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
